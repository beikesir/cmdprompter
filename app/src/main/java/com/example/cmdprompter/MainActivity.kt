package com.example.cmdprompter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cmdprompter.data.ConfigRepository
import com.example.cmdprompter.ui.screens.MainScreen
import com.example.cmdprompter.ui.theme.CmdPrompterTheme
import com.example.cmdprompter.viewmodel.MainViewModel
import com.example.cmdprompter.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = ConfigRepository(applicationContext)

        setContent {
            CmdPrompterTheme {
                val vm: MainViewModel = viewModel(factory = MainViewModelFactory(repository))
                MainScreen(vm = vm)
            }
        }
    }
}
