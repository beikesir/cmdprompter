package com.example.cmdprompter

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cmdprompter.data.ConfigRepository
import com.example.cmdprompter.ui.screens.MainScreen
import com.example.cmdprompter.ui.theme.CmdPrompterTheme
import com.example.cmdprompter.viewmodel.MainViewModel
import com.example.cmdprompter.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 边到边绘制：状态栏/导航栏由 Compose 侧的 insets 统一处理
        // （顶部终端区为深色，状态栏图标用浅色；底部为浅色，导航栏图标用深色）
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        val repository = ConfigRepository(applicationContext)

        setContent {
            CmdPrompterTheme {
                val vm: MainViewModel = viewModel(factory = MainViewModelFactory(repository))
                MainScreen(vm = vm)
            }
        }
    }
}
