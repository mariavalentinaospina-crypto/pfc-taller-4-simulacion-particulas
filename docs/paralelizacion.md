# Informe de paralelización

## Estrategia

Cada llamada trabaja sobre el intervalo semiabierto $[a,b)$ del arreglo original. Si su longitud supera el umbral efectivo $u=\max(1,\text{umbral})$, corta en $a+\lfloor(b-a)/2\rfloor$ y entrega las dos ramas a parallel. El resultado se combina al terminar ambas. El avance concatena las salidas de izquierda a derecha. La energía suma los parciales y el centro de masa suma los momentos y las masas de cada rama antes de dividir en la raíz.

El avance secuencial y las hojas usan pasoSegmento con Array.tabulate. Con umbral $n$, pasoParalelo llama directamente a pasoSecuencial. Para umbrales menores, cada nivel de concatenación copia referencias a las partículas ya construidas. Con $L$ niveles de reparto, estas copias agregan hasta $O(nL)$ operaciones y la concatenación de la raíz tiene coste lineal.

## Método de medición

App.scala construye sistemas deterministas de $250\,000$ y $1\,000\,000$ partículas antes de medir. Usa $dt=0.5$ y la mediana de siete mediciones con calentamiento de ScalaMeter. Alterna el orden entre el cálculo secuencial y el control sin reparto, con medianas separadas para los dos. La JVM detectó cuatro núcleos físicos y ocho procesadores lógicos en un AMD Ryzen 5 7520U, con Windows 11 y Java 17.

Para repetir la corrida aislada en PowerShell:

powershell
$env:JAVA_TOOL_OPTIONS = '-Xms768m -Xmx768m'
.\gradlew.bat run --no-daemon
Remove-Item Env:JAVA_TOOL_OPTIONS


## Resultados de la corrida con test run

| n | Umbral | Secuencial (ms) | Paralelo (ms) | Aceleración | Profundidad | Tiempo estimado |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 250000 | 250000 (control) | 7.306 | 8.321 | 0.878 | 250000 | 281250.00 |
| 250000 | 1024 | 7.306 | 7.246 | 1.008 | 985 | 32235.00 |
| 250000 | 8192 | 7.306 | 5.110 | 1.430 | 7818 | 39068.00 |
| 250000 | 32768 | 7.306 | 6.187 | 1.181 | 31253 | 62503.00 |
| 250000 | 131072 | 7.306 | 6.745 | 1.083 | 125001 | 156251.00 |
| 1000000 | 1000000 (control) | 45.443 | 142.162 | 0.320 | 1000000 | 1125000.00 |
| 1000000 | 1024 | 45.443 | 48.928 | 0.929 | 987 | 125987.00 |
| 1000000 | 8192 | 45.443 | 34.617 | 1.313 | 7820 | 132820.00 |
| 1000000 | 32768 | 45.443 | 35.556 | 1.278 | 31255 | 156255.00 |
| 1000000 | 131072 | 45.443 | 54.029 | 0.841 | 125003 | 250003.00 |

## Resultados de la corrida aislada con heap fijo

| n | Umbral | Secuencial (ms) | Paralelo (ms) | Aceleración | Profundidad | Tiempo estimado |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 250000 | 250000 (control) | 14.616 | 14.261 | 1.025 | 250000 | 281250.00 |
| 250000 | 1024 | 14.616 | 6.384 | 2.289 | 985 | 32235.00 |
| 250000 | 8192 | 14.616 | 7.898 | 1.851 | 7818 | 39068.00 |
| 250000 | 32768 | 14.616 | 6.881 | 2.124 | 31253 | 62503.00 |
| 250000 | 131072 | 14.616 | 6.320 | 2.313 | 125001 | 156251.00 |
| 1000000 | 1000000 (control) | 68.482 | 95.904 | 0.714 | 1000000 | 1125000.00 |
| 1000000 | 1024 | 68.482 | 91.587 | 0.748 | 987 | 125987.00 |
| 1000000 | 8192 | 68.482 | 104.592 | 0.655 | 7820 | 132820.00 |
| 1000000 | 32768 | 68.482 | 91.160 | 0.751 | 31255 | 156255.00 |
| 1000000 | 131072 | 68.482 | 29.227 | 2.343 | 125003 | 250003.00 |

## Modelo y comparación

La columna de tiempo estimado usa $T_p=D+n/p$, con $p=8$, en unidades abstractas del modelo. Para comparar la tendencia de aceleración usamos $n/T_p$ porque barrer el arreglo secuencialmente requiere $n$ movimientos en ese modelo. La fórmula aproxima el coste bajo un reparto ideal. El coste real incluye reservar arreglos y objetos, copiar referencias, sincronizar tareas y competir por ancho de banda de memoria. La fila de control tiene una aceleración teórica de $1$ porque ambas llamadas ejecutan el mismo avance.

## Lectura de los resultados

Con un millón de partículas, el modelo favorece el umbral $1024$ y estima una aceleración de $1\,000\,000/125\,987\approx7.94$. En la corrida aislada, el mejor tiempo medido fue $29.227$ ms con umbral $131072$, correspondiente a $2.343$ veces la referencia secuencial. Este umbral produce ocho hojas de $125\,000$ elementos. Con $1024$ se crean $1024$ hojas y hay diez niveles de concatenación, lo que aumenta el coste de planificación y copias. Con $250\,000$ partículas, los umbrales $1024$ y $131072$ quedaron a $0.064$ ms entre sí en la corrida aislada.

En la corrida con test run, el menor tiempo en ambos tamaños correspondió a $8192$. El cambio de umbral ganador entre corridas limita la confianza en un ganador universal. En el control, los coeficientes fueron $0.878$ y $0.320$ en la primera corrida y $1.025$ y $0.714$ en la corrida aislada, en orden creciente de tamaño. La equivalencia del código fija la expectativa de $1$, y la desviación observada para el millón de partículas indica que la medición es sensible a variaciones del entorno. La presión de memoria, la recolección de basura y la actividad del sistema pueden contribuir a esa variación. Los cocientes de ambas tablas se calcularon con sus tiempos originales.

La ley de Amdahl relaciona la aceleración $S$ con una fracción secuencial efectiva $f$ en $p$ procesadores:

$$
S=\frac{1}{f+(1-f)/p},
\qquad
f=\frac{1/S-1/p}{1-1/p}.
$$

Con $p=8$ y $S=2.343$, resulta $f\approx0.345$, cerca del $35\,\%$. Con $S=1.313$ de la otra corrida, resulta $f\approx0.728$. Estas cifras incluyen el efecto de las sobrecargas y son sensibles al control. Con la primera estimación, el límite ideal sería $1/f\approx2.90$. Agregar núcleos reduce el término paralelo $(1-f)/p$ y deja un coste residual. En esta implementación contribuyen a ese coste la concatenación final, la coordinación de tareas y la saturación de memoria