package com.example.lifestatus

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.* // O material normal
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// --- MODELO DE DATOS (El "Paciente") ---
data class PatientResult(val results: List<Patient>)
data class Patient(
    val id: Int,
    val name: String,
    val status: String, // Alive, Dead, unknown
    val species: String,
    val image: String
)

// --- API INTERFACE ---
interface MedicalApi {
    @GET("character")
    suspend fun getPatients(): PatientResult
}

// --- INSTANCIA RETROFIT (Singleton simple) ---
object RetrofitClient {
    val api: MedicalApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://rickandmortyapi.com/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MedicalApi::class.java)
    }
}

// --- VIEWMODEL (Manejo de Estado) ---
class MedicalViewModel : ViewModel() {
    // Estado: Lista de pacientes y si está cargando
    var patients by mutableStateOf<List<Patient>>(emptyList())
    var isLoading by mutableStateOf(false)

    init {
        fetchPatients()
    }

    private fun fetchPatients() {
        isLoading = true
        // Usamos viewModelScope directamente para lanzar la corrutina
        viewModelScope.launch {
            try {
                // Llamamos a la API
                val result = RetrofitClient.api.getPatients()
                // Actualizamos la lista
                patients = result.results
            } catch (e: Exception) {
                // Si falla, por ahora no hacemos nada o imprimimos error
                e.printStackTrace()
            } finally {
                // Termine bien o mal, quitamos el indicador de carga
                isLoading = false
            }
        }
    }
}

// --- UI: PANTALLA PRINCIPAL (Lista de Pacientes) ---
@Composable
fun PatientListScreen(navController: androidx.navigation.NavController, viewModel: MedicalViewModel) {
    // Top Bar simulado
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "🏥 LifeStatus: Pacientes",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        if (viewModel.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn {
                items(viewModel.patients) { patient ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable {
                                // Navegación al detalle pasando el ID
                                navController.navigate("detail/${patient.id}")
                            },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = patient.image,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(text = patient.name, style = MaterialTheme.typography.titleMedium)
                                // Lógica visual "Médica"
                                val statusColor = if (patient.status == "Alive") Color.Green else Color.Red
                                Text(text = "Estado: ${patient.status}", color = statusColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- UI: PANTALLA DETALLE (Historia Clínica) ---
@Composable
fun PatientDetailScreen(patientId: String?, viewModel: MedicalViewModel, navController: androidx.navigation.NavController) {
    // Buscamos el paciente en la lista que ya tenemos en el ViewModel (evita otra llamada a API)
    val patient = viewModel.patients.find { it.id.toString() == patientId }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = { navController.popBackStack() }) { Text("Volver") }

        patient?.let {
            Spacer(modifier = Modifier.height(16.dp))
            AsyncImage(
                model = it.image,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(300.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Ficha Médica: #${it.id}", style = MaterialTheme.typography.headlineSmall)
            Text(text = "Paciente: ${it.name}", style = MaterialTheme.typography.headlineMedium)
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text(text = "🧬 Genética: ${it.species}")
            Text(text = "🩺 Diagnóstico: ${it.status}",
                color = if (it.status == "Alive") Color.Green else Color.Red,
                style = MaterialTheme.typography.titleLarge)
        } ?: Text("Paciente no encontrado")
    }
}

// --- MAIN ACTIVITY (Navegación) ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val viewModel: MedicalViewModel = viewModel() // ViewModel compartido

            NavHost(navController = navController, startDestination = "list") {
                composable("list") {
                    PatientListScreen(navController, viewModel)
                }
                composable("detail/{patientId}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("patientId")
                    PatientDetailScreen(id, viewModel, navController)
                }
            }
        }
    }
}