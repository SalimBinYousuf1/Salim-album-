package com.example.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiquidGlassSystem(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "asgl_transition")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "asgl_time"
    )

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val runtimeShader = remember {
                try {
                    RuntimeShader(ASGL_SHADER)
                } catch (e: Exception) {
                    null
                }
            }

            if (runtimeShader != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    runtimeShader.setFloatUniform("resolution", size.width, size.height)
                    runtimeShader.setFloatUniform("time", time)
                    drawRect(brush = ShaderBrush(runtimeShader))
                }
            } else {
                FallbackLiquidBackground(time = time)
            }
        } else {
            FallbackLiquidBackground(time = time)
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            content = content
        )
    }
}

@Composable
private fun FallbackLiquidBackground(time: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val t = time * 0.1f

        drawRect(Color(0xFFFCFCFD))

        // Ambient gentle color fields
        val redX = (0.5f + 0.35f * sin(t * 0.4f)) * w
        val redY = (0.5f + 0.35f * cos(t * 0.3f)) * h
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x28EA4335), Color.Transparent),
                center = Offset(redX, redY),
                radius = w * 0.7f
            ),
            radius = w * 0.7f,
            center = Offset(redX, redY)
        )

        val blueX = (0.5f + 0.35f * cos(t * 0.35f)) * w
        val blueY = (0.5f + 0.35f * sin(t * 0.45f)) * h
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x284285F4), Color.Transparent),
                center = Offset(blueX, blueY),
                radius = w * 0.75f
            ),
            radius = w * 0.75f,
            center = Offset(blueX, blueY)
        )

        val greenX = (0.5f + 0.35f * sin(t * 0.5f)) * w
        val greenY = (0.5f + 0.35f * sin(t * 0.25f)) * h
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x2834A853), Color.Transparent),
                center = Offset(greenX, greenY),
                radius = w * 0.7f
            ),
            radius = w * 0.7f,
            center = Offset(greenX, greenY)
        )

        val yellowX = (0.5f + 0.35f * cos(t * 0.2f)) * w
        val yellowY = (0.5f + 0.35f * cos(t * 0.4f)) * h
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x25FBBC05), Color.Transparent),
                center = Offset(yellowX, yellowY),
                radius = w * 0.8f
            ),
            radius = w * 0.8f,
            center = Offset(yellowX, yellowY)
        )
    }
}

private const val ASGL_SHADER = """
uniform float2 resolution;
uniform float time;

float hash21(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);

    f = f * f * (3.0 - 2.0 * f);

    float a = hash21(i);
    float b = hash21(i + float2(1.0, 0.0));
    float c = hash21(i + float2(0.0, 1.0));
    float d = hash21(i + float2(1.0, 1.0));

    return mix(
        mix(a, b, f.x),
        mix(c, d, f.x),
        f.y
    );
}

float fbm(float2 p, float seed) {
    float value = 0.0;
    float amplitude = 0.55;

    float2 shift = float2(
        17.1 + seed * 3.7,
        9.2 + seed * 5.1
    );

    value += noise(p + shift) * amplitude;

    p = p * 2.03 + shift;
    value += noise(p) * 0.28;

    p = p * 2.07 - shift * 0.43;
    value += noise(p) * 0.12;

    p = p * 2.11 + shift * 0.27;
    value += noise(p) * 0.05;

    return value;
}

float cloudDensity(
    float2 p,
    float seed,
    float stretch,
    float phase
) {
    float2 warp = float2(
        fbm(
            p * 0.72 +
                float2(
                    phase * 0.17,
                    -phase * 0.11
                ),
            seed + 4.0
        ),
        fbm(
            p * 0.72 +
                float2(
                    -phase * 0.13,
                    phase * 0.19
                ),
            seed + 8.0
        )
    );

    float2 warped = p + (warp - 0.5) * 1.65;

    warped.x *= stretch;

    float field = fbm(
        warped +
            float2(
                phase * 0.07,
                -phase * 0.05
            ),
            seed
    );

    float detail = noise(
        warped * 5.5 +
            phase * 0.13 +
            seed
    );

    float density = smoothstep(
        0.39,
        0.70,
        field + (detail - 0.5) * 0.16
    );

    return density * (0.78 + 0.22 * detail);
}

float2 redCenter(float t) {
    return float2(
        0.50 + 0.57 * sin(t * 0.071),
        0.50 + 0.62 * cos(t * 0.053 + 1.2)
    );
}

float2 blueCenter(float t) {
    return float2(
        0.50 + 0.68 * cos(t * 0.047 + 2.4),
        0.50 + 0.58 * sin(t * 0.081)
    );
}

float2 greenCenter(float t) {
    return float2(
        0.50 + 0.62 * sin(t * 0.059 + 4.1),
        0.50 + 0.68 * sin(t * 0.039 + 0.7)
    );
}

float2 yellowCenter(float t) {
    return float2(
        0.50 + 0.72 * cos(t * 0.033 + 5.0),
        0.50 + 0.56 * cos(t * 0.067 + 2.1)
    );
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / resolution;

    float aspect = resolution.x /
        max(resolution.y, 1.0);

    float2 p = uv;
    p.x *= aspect;

    float t = time;

    float2 rc = redCenter(t);
    float2 bc = blueCenter(t);
    float2 gc = greenCenter(t);
    float2 yc = yellowCenter(t);

    rc.x *= aspect;
    bc.x *= aspect;
    gc.x *= aspect;
    yc.x *= aspect;

    float red = cloudDensity(
        (p - rc) * 2.05,
        1.7,
        0.78 + 0.12 * sin(t * 0.11),
        t * 0.31
    );

    float blue = cloudDensity(
        (p - bc) * 2.55,
        8.3,
        1.05 + 0.16 * cos(t * 0.09),
        t * 0.27
    );

    float green = cloudDensity(
        (p - gc) * 2.25,
        15.6,
        0.67 + 0.18 * sin(t * 0.13),
        t * 0.22
    );

    float yellow = cloudDensity(
        (p - yc) * 3.10,
        22.4,
        1.30 + 0.20 * cos(t * 0.07),
        t * 0.36
    );

    float3 color = float3(
        0.985,
        0.988,
        1.0
    );

    color = mix(
        color,
        float3(0.92, 0.015, 0.02),
        red * 0.24
    );

    color = mix(
        color,
        float3(0.015, 0.10, 0.95),
        blue * 0.24
    );

    color = mix(
        color,
        float3(0.03, 0.72, 0.08),
        green * 0.24
    );

    color = mix(
        color,
        float3(0.95, 0.68, 0.015),
        yellow * 0.20
    );

    float total = clamp(
        red * 0.34 +
            blue * 0.34 +
            green * 0.34 +
            yellow * 0.30,
        0.0,
        0.86
    );

    color = mix(
        color,
        color * (1.0 - total * 0.08),
        0.35
    );

    return half4(
        half3(clamp(color, 0.0, 1.0)),
        1.0
    );
}
"""
