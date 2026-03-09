package blu.macaw.velvetwall.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import blu.macaw.velvetwall.data.OnboardingStep
import blu.macaw.velvetwall.data.tutorialSteps
import blu.macaw.velvetwall.ui.theme.RoyalCyan
import blu.macaw.velvetwall.ui.theme.VelvetBlack
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VelvetTutorialScreen(onTutorialFinished: () -> Unit) {
    val pagerState = rememberPagerState { tutorialSteps.size }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Indicador de Progresso no Topo
        Row(
            Modifier.height(50.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(tutorialSteps.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) RoyalCyan else Color.DarkGray
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(10.dp)
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            TutorialContent(tutorialSteps[page])
        }

        // Botão de Ação Dinâmico
        Button(
            onClick = {
                if (pagerState.currentPage < tutorialSteps.size - 1) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    onTutorialFinished()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RoyalCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (pagerState.currentPage == tutorialSteps.size - 1) "ATIVAR BLINDAGEM" else "PRÓXIMO",
                color = VelvetBlack,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TutorialContent(step: OnboardingStep) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.Block,
            contentDescription = null,
            tint = step.highlightColor,
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = step.title,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = step.description,
            color = Color.Gray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}