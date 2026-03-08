package com.example.animationpokemon

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Timings
// ─────────────────────────────────────────────────────────────────────────────
private const val LIQUID_MS       = 700L
private const val LIQUID_BULGE_MS = 300
private const val PAUSE_APRES_MS  = 130L

// ─────────────────────────────────────────────────────────────────────────────
// Data
// ─────────────────────────────────────────────────────────────────────────────
data class Pokemon(
    val nom: String,
    val imageRes: Int,
    val couleurFond: Color,
    val couleurAccent: Color,
    val couleurDark: Color,
    val type: String,
    val type2: String = "",
    val capacite: String,
    val taille: String,
    val poids: String,
    val description: String,
    val hp: Int,
    val attack: Int,
    val defense: Int
)

val listePokemon = listOf(
    Pokemon(
        nom = "Pikachu", imageRes = R.drawable.pikachu,
        couleurFond = Color(0xFFFFD600), couleurAccent = Color(0xFFFFA000),
        couleurDark = Color(0xFFE65100),
        type = "ELECTRIC",
        capacite = "Static", taille = "0.4 m", poids = "6.0 kg",
        description = "Pikachu, an Electric-type Pokémon. It stores electricity in its cheeks and releases it as lightning bolts.",
        hp = 35, attack = 55, defense = 40
    ),
    Pokemon(
        nom = "Squirtle", imageRes = R.drawable.squirtle,
        couleurFond = Color(0xFF29B6F6), couleurAccent = Color(0xFF0277BD),
        couleurDark = Color(0xFF01579B),
        type = "WATER",
        capacite = "Torrent", taille = "0.5 m", poids = "9.0 kg",
        description = "Squirtle, a Water-type Pokémon. It shoots water at prey while in the water using its jet-like shell.",
        hp = 44, attack = 48, defense = 65
    ),
    Pokemon(
        nom = "Charmander", imageRes = R.drawable.charmander,
        couleurFond = Color(0xFFFF6B35), couleurAccent = Color(0xFFE64A19),
        couleurDark = Color(0xFFBF360C),
        type = "FIRE",
        capacite = "Blaze", taille = "0.6 m", poids = "8.5 kg",
        description = "Charmander, the Fire-type Pokémon. The flame on its tail indicates its emotions. If it's healthy, the flame burns brightly.",
        hp = 39, attack = 52, defense = 43
    ),
    Pokemon(
        nom = "Bulbasaur", imageRes = R.drawable.bulbasaur,
        couleurFond = Color(0xFF5DBE6E), couleurAccent = Color(0xFF2E8B57),
        couleurDark = Color(0xFF1B5E20),
        type = "GRASS", type2 = "POISON",
        capacite = "Overgrow", taille = "0.7 m", poids = "6.9 kg",
        description = "Bulbasaur, the Seed Pokémon. A strange seed was planted on its back at birth. The plant sprouts and grows with this Pokémon.",
        hp = 45, attack = 49, defense = 49
    )
)

// ─────────────────────────────────────────────────────────────────────────────
// Liquid path — double bezier S-wave
// ─────────────────────────────────────────────────────────────────────────────
fun buildLiquidPath(size: Size, progress: Float, bulge: Float, direction: Int): Path {
    val w = size.width; val h = size.height
    val bulgeAmt = w * 0.38f * bulge * (1f - progress * 0.80f)
    val bulge2   = bulgeAmt * 0.45f
    return Path().apply {
        if (direction == 1) {
            val fx = w * (1f - progress)
            moveTo(w, 0f); lineTo(fx, 0f)
            cubicTo(fx + bulgeAmt, h * 0.20f, fx + bulgeAmt, h * 0.50f, fx + bulge2, h * 0.65f)
            cubicTo(fx - bulge2, h * 0.80f, fx + bulgeAmt * 0.3f, h * 0.90f, fx, h)
            lineTo(w, h); close()
        } else {
            val fx = w * progress
            moveTo(0f, 0f); lineTo(fx, 0f)
            cubicTo(fx - bulgeAmt, h * 0.20f, fx - bulgeAmt, h * 0.50f, fx - bulge2, h * 0.65f)
            cubicTo(fx + bulge2, h * 0.80f, fx - bulgeAmt * 0.3f, h * 0.90f, fx, h)
            lineTo(0f, h); close()
        }
    }
}

fun typeColor(type: String): Color = when (type) {
    "FIRE"     -> Color(0xFFFF6B35)
    "WATER"    -> Color(0xFF29B6F6)
    "GRASS"    -> Color(0xFF5DBE6E)
    "ELECTRIC" -> Color(0xFFFFD600)
    "POISON"   -> Color(0xFFAB47BC)
    else       -> Color(0xFF78909C)
}

fun loopIndex(current: Int, delta: Int, size: Int): Int = (current + delta + size) % size

// ─────────────────────────────────────────────────────────────────────────────
// Main screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PokemonTransitionScreen(indexInitial: Int = 0) {
    val scope = rememberCoroutineScope()

    var indexCourant by remember { mutableStateOf(indexInitial) }
    var indexSuivant by remember { mutableStateOf(indexInitial) }
    var direction    by remember { mutableStateOf(1) }
    var pageKey      by remember { mutableStateOf(0) }

    val progress = remember { Animatable(0f) }
    val bulge    = remember { Animatable(0f) }
    var dragTotal by remember { mutableStateOf(0f) }
    var dialogOuvert by remember { mutableStateOf(false) }

    val pokeballRightRot   = remember { Animatable(0f) }
    val pokeballRightScale = remember { Animatable(1f) }
    val pokeballLeftRot    = remember { Animatable(0f) }
    val pokeballLeftScale  = remember { Animatable(1f) }

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val bgScale by infiniteTransition.animateFloat(
        initialValue = 0.88f, targetValue = 1.06f, label = "bgScale",
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val bgAlpha by infiniteTransition.animateFloat(
        initialValue = 0.10f, targetValue = 0.20f, label = "bgAlpha",
        animationSpec = infiniteRepeatable(tween(3200), RepeatMode.Reverse)
    )

    fun animatePokeball(
        rotAnim: Animatable<Float, AnimationVector1D>,
        scaleAnim: Animatable<Float, AnimationVector1D>,
        reversed: Boolean = false
    ) {
        scope.launch {
            launch { scaleAnim.animateTo(1.45f, tween(120)); scaleAnim.animateTo(1f, tween(200)) }
            val target = if (reversed) rotAnim.value - 360f else rotAnim.value + 360f
            rotAnim.animateTo(target, tween(LIQUID_MS.toInt(), easing = FastOutSlowInEasing))
        }
    }

    fun lancerTransition(vers: Int, dir: Int) {
        if (progress.isRunning) return
        val looped = loopIndex(indexCourant, if (dir == 1) 1 else -1, listePokemon.size)
        indexSuivant = looped; direction = dir
        if (dir == 1) animatePokeball(pokeballRightRot, pokeballRightScale)
        else          animatePokeball(pokeballLeftRot, pokeballLeftScale, reversed = true)
        scope.launch {
            launch { bulge.animateTo(1f, tween(LIQUID_BULGE_MS, easing = FastOutSlowInEasing)) }
            progress.animateTo(1f, tween(LIQUID_MS.toInt(), easing = FastOutSlowInEasing))
            bulge.snapTo(0f); indexCourant = looped; progress.snapTo(0f); pageKey++
        }
    }

    fun navigateTo(vers: Int) {
        if (progress.isRunning || vers == indexCourant) return
        val dir = if (vers > indexCourant) 1 else -1
        indexSuivant = vers; direction = dir
        if (dir == 1) animatePokeball(pokeballRightRot, pokeballRightScale)
        else          animatePokeball(pokeballLeftRot, pokeballLeftScale, reversed = true)
        scope.launch {
            launch { bulge.animateTo(1f, tween(LIQUID_BULGE_MS, easing = FastOutSlowInEasing)) }
            progress.animateTo(1f, tween(LIQUID_MS.toInt(), easing = FastOutSlowInEasing))
            bulge.snapTo(0f); indexCourant = vers; progress.snapTo(0f); pageKey++
        }
    }

    val pokemon = listePokemon[indexCourant]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(indexCourant) {
                detectHorizontalDragGestures(
                    onDragStart  = { dragTotal = 0f },
                    onDragEnd    = { dragTotal = 0f },
                    onDragCancel = { dragTotal = 0f },
                    onHorizontalDrag = { _, delta ->
                        if (!dialogOuvert) {
                            dragTotal += delta
                            when {
                                dragTotal < -80 -> { dragTotal = 0f; lancerTransition(indexCourant, 1) }
                                dragTotal > 80  -> { dragTotal = 0f; lancerTransition(indexCourant, -1) }
                            }
                        }
                    }
                )
            }
    ) {
        // Background gradient
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(pokemon.couleurFond, pokemon.couleurAccent, pokemon.couleurDark))
            )
        )

        // Pulsing circle top-right
        Box(
            modifier = Modifier.size(440.dp).align(Alignment.TopEnd)
                .offset(x = 110.dp, y = (-90).dp).scale(bgScale),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.White.copy(alpha = bgAlpha)))
        }

        // Pulsing circle bottom-left
        Box(
            modifier = Modifier.size(300.dp).align(Alignment.BottomStart)
                .offset(x = (-70).dp, y = 90.dp).scale(1f / bgScale),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.White.copy(alpha = bgAlpha * 0.6f)))
        }

        // Liquid transition
        if (progress.value > 0f) {
            Box(
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    clip = true
                    shape = GenericShape { sz, _ ->
                        addPath(buildLiquidPath(sz, progress.value, bulge.value, direction))
                    }
                }
            ) { PageFondSeul(listePokemon[indexSuivant]) }
        }

        // Page
        if (progress.value == 0f && !progress.isRunning) {
            PokemonPageAnimee(
                pokemon            = pokemon,
                indexGlobal        = indexCourant,
                pageKey            = pageKey,
                onNavigate         = { v -> navigateTo(v) },
                onNext             = { lancerTransition(indexCourant, 1) },
                onPrev             = { lancerTransition(indexCourant, -1) },
                onOpenPokedex      = { dialogOuvert = true },
                pokeballRightRot   = pokeballRightRot.value,
                pokeballRightScale = pokeballRightScale.value,
                pokeballLeftRot    = pokeballLeftRot.value,
                pokeballLeftScale  = pokeballLeftScale.value
            )
        }

        // Dialog
        if (dialogOuvert) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.62f)))
            PokedexDialogCard(pokemon = pokemon, onClose = { dialogOuvert = false })
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Background during transition
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PageFondSeul(pokemon: Pokemon) {
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(pokemon.couleurFond, pokemon.couleurAccent, pokemon.couleurDark))
        )
    ) {
        Box(
            modifier = Modifier.size(440.dp).align(Alignment.TopEnd)
                .offset(x = 110.dp, y = (-90).dp)
                .clip(CircleShape).background(Color.White.copy(0.13f))
        )
        Box(
            modifier = Modifier.size(300.dp).align(Alignment.BottomStart)
                .offset(x = (-70).dp, y = 90.dp)
                .clip(CircleShape).background(Color.White.copy(0.08f))
        )
        Image(
            painterResource(R.drawable.pokemon), "Logo",
            modifier = Modifier.align(Alignment.TopCenter)
                .padding(top = 52.dp).width(200.dp).height(72.dp),
            contentScale = ContentScale.Fit
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pokédex dialog
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PokedexDialogCard(pokemon: Pokemon, onClose: () -> Unit) {
    val slideIn = remember { Animatable(400f) }
    val alpha   = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { slideIn.animateTo(0f, tween(420, easing = FastOutSlowInEasing)) }
        launch { alpha.animateTo(1f, tween(320)) }
    }
    Box(
        modifier = Modifier.fillMaxSize()
            .graphicsLayer { translationY = slideIn.value; this.alpha = alpha.value },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Brush.verticalGradient(listOf(pokemon.couleurFond, pokemon.couleurAccent, pokemon.couleurDark)))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(Color(0xFFE53935)))
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color.White.copy(0.35f)))
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color.White.copy(0.35f)))
                    Box(Modifier.size(16.dp).clip(CircleShape).background(Color(0xFF43A047)))
                }
                Box(contentAlignment = Alignment.Center) {
                    Box(Modifier.size(160.dp).clip(CircleShape).background(Color.White.copy(0.18f)).blur(24.dp))
                    Image(painterResource(pokemon.imageRes), pokemon.nom, Modifier.size(148.dp), contentScale = ContentScale.Fit)
                }
                Text(
                    pokemon.nom, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                    style = TextStyle(shadow = Shadow(Color.Black.copy(0.3f), Offset(2f, 2f), 6f))
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeBadge(pokemon.type)
                    if (pokemon.type2.isNotEmpty()) TypeBadge(pokemon.type2)
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(0.22f))
                        .padding(horizontal = 18.dp, vertical = 14.dp).fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoRowDialog("Ability", pokemon.capacite)
                        InfoRowDialog("Height",  pokemon.taille)
                        InfoRowDialog("Weight",  pokemon.poids)
                        Divider(color = Color.White.copy(0.2f), thickness = 1.dp)
                        StatBar("HP",  pokemon.hp,      Color(0xFF66BB6A))
                        StatBar("ATK", pokemon.attack,  Color(0xFFEF5350))
                        StatBar("DEF", pokemon.defense, Color(0xFF42A5F5))
                        Spacer(Modifier.height(2.dp))
                        Text(pokemon.description, fontSize = 12.sp, color = Color.White.copy(0.82f), lineHeight = 17.sp)
                    }
                }
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(0.75f))
                ) {
                    Text("Close Pokédex", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun TypeBadge(type: String) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(typeColor(type).copy(0.88f))
            .padding(horizontal = 14.dp, vertical = 5.dp)
    ) {
        Text(type, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun StatBar(label: String, value: Int, color: Color) {
    val animatedWidth = remember { Animatable(0f) }
    LaunchedEffect(value) {
        animatedWidth.animateTo((value / 100f).coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing))
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
        Text(value.toString(), color = Color.White.copy(0.7f), fontSize = 11.sp, modifier = Modifier.width(28.dp))
        Box(modifier = Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(0.2f))) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animatedWidth.value)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(color, color.copy(0.55f)))))
        }
    }
}

@Composable
fun InfoRowDialog(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label  ", fontWeight = FontWeight.Bold, color = Color.White.copy(0.6f), fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pokemon page — updateTransition pour Animation Preview panneau
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PokemonPageAnimee(
    pokemon: Pokemon,
    indexGlobal: Int,
    pageKey: Int,
    onNavigate: (Int) -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onOpenPokedex: () -> Unit,
    pokeballRightRot: Float   = 0f,
    pokeballRightScale: Float = 1f,
    pokeballLeftRot: Float    = 0f,
    pokeballLeftScale: Float  = 1f
) {
    // ── animStarted : false → true après PAUSE_APRES_MS ──────────────────────
    // C'est le seul état qui pilote updateTransition
    var animStarted by remember(pageKey) { mutableStateOf(false) }

    LaunchedEffect(pageKey) {
        animStarted = false
        delay(PAUSE_APRES_MS)
        animStarted = true
    }

    // ── updateTransition — visible dans le panneau Animation Preview ──────────
    val transition = updateTransition(targetState = animStarted, label = "pokemon_entry")

    // Nom — descend + scale up + fondu
    val nomOffsetY by transition.animateFloat(
        label = "nom_offsetY",
        transitionSpec = { tween(820, easing = LinearOutSlowInEasing) }
    ) { if (it) 0f else -180f }

    val nomAlpha by transition.animateFloat(
        label = "nom_alpha",
        transitionSpec = { tween(520) }
    ) { if (it) 1f else 0f }

    val nomScale by transition.animateFloat(
        label = "nom_scale",
        transitionSpec = { tween(700, easing = FastOutSlowInEasing) }
    ) { if (it) 1f else 0.6f }

    // Image — rebond (monte, dépasse, retombe) + scale up + fondu
    val imageOffsetY by transition.animateFloat(
        label = "image_offsetY",
        transitionSpec = {
            if (targetState) keyframes {
                durationMillis = 700
                500f at 0   using FastOutSlowInEasing
                -55f at 440 using FastOutSlowInEasing
                0f   at 700
            } else tween(300)
        }
    ) { if (it) 0f else 500f }

    val imageAlpha by transition.animateFloat(
        label = "image_alpha",
        transitionSpec = { tween(280) }
    ) { if (it) 1f else 0f }

    val imageScale by transition.animateFloat(
        label = "image_scale",
        transitionSpec = { tween(620, easing = FastOutSlowInEasing) }
    ) { if (it) 1f else 0.65f }

    // Bouton — lève depuis le bas avec délai 80ms simulé + fondu
    val boutonOffsetY by transition.animateFloat(
        label = "bouton_offsetY",
        transitionSpec = {
            if (targetState) keyframes {
                durationMillis = 900
                200f at 0
                200f at 80  using LinearOutSlowInEasing   // délai 80ms simulé
                0f   at 900
            } else tween(300)
        }
    ) { if (it) 0f else 200f }

    val boutonAlpha by transition.animateFloat(
        label = "bouton_alpha",
        transitionSpec = {
            if (targetState) keyframes {
                durationMillis = 600
                0f at 0
                0f at 80    // invisible pendant 80ms
                1f at 600
            } else tween(300)
        }
    ) { if (it) 1f else 0f }

    // ── Animations infinies — inchangées ─────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -14f, targetValue = 14f, label = "floatY",
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val bgRot by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f, label = "bgRot",
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart)
    )
    val bgPokeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f, targetValue = 0.13f, label = "bgPokeA",
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse)
    )
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = -400f, targetValue = 400f, label = "shimmer",
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart)
    )
    val nameScale by infiniteTransition.animateFloat(
        initialValue = 0.97f, targetValue = 1.03f, label = "nameScale",
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )

    // ─────────────────────────────────────────────────────────────────────────
    // UI — identique à l'original, seuls les .value remplacés par transition
    // ─────────────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {

        // Corner pokeball top-right
        Box(
            modifier = Modifier.size(440.dp).align(Alignment.TopEnd)
                .offset(x = 110.dp, y = (-90).dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painterResource(R.drawable.pokeball), null,
                modifier = Modifier.fillMaxSize().rotate(bgRot).graphicsLayer { alpha = 0.18f },
                contentScale = ContentScale.Fit
            )
        }

        // Corner pokeball bottom-left
        Box(
            modifier = Modifier.size(300.dp).align(Alignment.BottomStart)
                .offset(x = (-70).dp, y = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painterResource(R.drawable.pokeball), null,
                modifier = Modifier.fillMaxSize().rotate(-bgRot).graphicsLayer { alpha = 0.14f },
                contentScale = ContentScale.Fit
            )
        }

        // Big center bg pokeball
        Image(
            painterResource(R.drawable.pokeball), null,
            modifier = Modifier.size(360.dp).align(Alignment.Center).offset(y = 30.dp)
                .rotate(bgRot).graphicsLayer { alpha = bgPokeAlpha },
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp).statusBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painterResource(R.drawable.pokemon), "Logo",
                    modifier = Modifier.width(200.dp).height(72.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // Nom + badges — utilisent les valeurs de transition
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.graphicsLayer {
                    translationY = nomOffsetY   // ← transition
                    alpha        = nomAlpha     // ← transition
                    scaleX       = nomScale     // ← transition
                    scaleY       = nomScale     // ← transition
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = pokemon.nom,
                        fontSize = 50.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        color = Color.Black.copy(0.35f),
                        modifier = Modifier.offset(x = 3.dp, y = 5.dp).blur(8.dp)
                    )
                    Text(
                        text = pokemon.nom,
                        fontSize = 50.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.graphicsLayer {
                            scaleX = nameScale
                            scaleY = nameScale
                        },
                        style = TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White,
                                    Color.White.copy(0.55f),
                                    Color.White.copy(0.90f),
                                    Color.White.copy(0.55f),
                                    Color.White,
                                ),
                                start = Offset(shimmerX - 150f, 0f),
                                end   = Offset(shimmerX + 150f, 80f)
                            )
                        )
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeBadge(pokemon.type)
                    if (pokemon.type2.isNotEmpty()) TypeBadge(pokemon.type2)
                }
            }

            // Image — utilise les valeurs de transition
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(280.dp).clip(CircleShape).background(Color.White.copy(0.10f)).blur(32.dp))
                Image(
                    painter = painterResource(pokemon.imageRes),
                    contentDescription = pokemon.nom,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(0.80f).aspectRatio(1f).graphicsLayer {
                        translationY = imageOffsetY + floatY   // ← transition + float infini
                        alpha        = imageAlpha              // ← transition
                        scaleX       = imageScale              // ← transition
                        scaleY       = imageScale              // ← transition
                    }
                )
            }

            // Bouton + dots — utilisent les valeurs de transition
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.graphicsLayer {
                    translationY = boutonOffsetY   // ← transition
                    alpha        = boutonAlpha     // ← transition
                }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    listePokemon.forEachIndexed { i, _ ->
                        Box(
                            modifier = Modifier
                                .size(if (i == indexGlobal) 28.dp else 8.dp)
                                .clip(if (i == indexGlobal) RoundedCornerShape(50) else CircleShape)
                                .background(if (i == indexGlobal) Color.White else Color.White.copy(0.35f))
                                .clickable { if (i != indexGlobal) onNavigate(i) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (i == indexGlobal) {
                                Text("${i + 1}", fontSize = 10.sp, color = pokemon.couleurAccent, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                Button(
                    onClick = onOpenPokedex,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp).height(60.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(0.68f)),
                    elevation = ButtonDefaults.buttonElevation(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Image(painterResource(R.drawable.pokeball), null, Modifier.size(22.dp))
                        Text("• View ${pokemon.nom} in Pokédex", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Pokéball RIGHT
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd).padding(end = 14.dp).size(64.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color.White.copy(0.32f), Color.White.copy(0.08f))))
                .clickable { onNext() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painterResource(R.drawable.pokeball), "Next",
                modifier = Modifier.size(40.dp).rotate(pokeballRightRot).scale(pokeballRightScale)
            )
        }

        // Pokéball LEFT
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart).padding(start = 14.dp).size(64.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color.White.copy(0.32f), Color.White.copy(0.08f))))
                .clickable { onPrev() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painterResource(R.drawable.pokeball), "Previous",
                modifier = Modifier.size(40.dp)
                    .scale(scaleX = -1f, scaleY = 1f)
                    .rotate(pokeballLeftRot)
                    .scale(pokeballLeftScale)
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row {
        Text("$label : ", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        Text(value, color = Color.White.copy(0.9f), fontSize = 14.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true, name = "Bulbasaur")
@Composable fun PreviewBulbasaur() { PokemonTransitionScreen(0) }

@Preview(showBackground = true, showSystemUi = true, name = "Charmander")
@Composable fun PreviewCharmander() { PokemonTransitionScreen(1) }

@Preview(showBackground = true, showSystemUi = true, name = "Squirtle")
@Composable fun PreviewSquirtle() { PokemonTransitionScreen(2) }

@Preview(showBackground = true, showSystemUi = true, name = "Pikachu")
@Composable fun PreviewPikachu() { PokemonTransitionScreen(3) }