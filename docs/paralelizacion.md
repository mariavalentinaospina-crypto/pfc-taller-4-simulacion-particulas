# Informe de paralelización

## Estrategia

Cada llamada trabaja sobre el intervalo semiabierto $[a,b)$ del arreglo
original. Si su longitud es menor o igual al umbral efectivo
$u=\max(1,\text{umbral})$, procesa el tramo en secuencial. Si lo supera,
corta en $a+\lfloor(b-a)/2\rfloor$. `parallel` programa la rama derecha,
evalúa la izquierda en el hilo actual y espera a la derecha con `join`.
Ambas ramas se combinan según el resultado que calculan.

| Función | Resultado de una hoja | Combinación |
| --- | --- | --- |
| `pasoParalelo` | Arreglo del tramo movido | Concatenación en orden de índices |
| `energiaParalela` | Suma de energías del tramo | Suma de los dos parciales |
| `centroDeMasaParalelo` | Suma ponderada de posiciones y masa del tramo | Suma por componentes y una división en la raíz |

El avance secuencial y las hojas usan `pasoSegmento` con `Array.tabulate`. Con umbral $n$, `pasoParalelo` llama directamente a `pasoSecuencial`. Para umbrales menores, cada nivel de concatenación copia referencias a las partículas ya construidas. Con $L$ niveles de reparto, estas copias agregan hasta $O(nL)$ operaciones y la concatenación de la raíz tiene coste lineal.

## Método de medición

`App.scala` construye sistemas deterministas de $250\,000$ y $1\,000\,000$
partículas antes de medir. Mide un avance con $dt=0.5$ y reporta la mediana
de siete mediciones con calentamiento de ScalaMeter por variante. En las
muestras de control alterna el orden entre la llamada secuencial y la
paralela con umbral $n$. La aceleración de cada fila divide la mediana
secuencial del tamaño por la mediana de la variante. Se usó un AMD Ryzen 5
7520U con cuatro núcleos físicos y ocho procesadores lógicos disponibles
para la JVM, con Windows 11 y Java 17.

Para repetir la corrida aislada en PowerShell:

```powershell
$env:JAVA_TOOL_OPTIONS = '-Xms768m -Xmx768m'
.\gradlew.bat run --no-daemon
Remove-Item Env:JAVA_TOOL_OPTIONS
```

## Resultados de la corrida con `test run`

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

La columna de tiempo estimado usa $T_p=D+n/p$, con $p=8$, en unidades
abstractas del modelo. Para comparar los umbrales con reparto usamos
$n/T_p$, donde $n$ es el número de movimientos del avance secuencial.
El control tiene una referencia de $1$ porque las dos llamadas recorren
el arreglo de la misma forma y ejecutan el avance en un solo hilo.

## Lectura de los resultados

### Comparación para un millón de partículas

| Umbral | $n/T_8$ del modelo | Aceleración con `test run` | Aceleración con heap fijo |
| ---: | ---: | ---: | ---: |
| 1024 | 7.94 | 0.929 | 0.748 |
| 8192 | 7.53 | 1.313 | 0.655 |
| 32768 | 6.40 | 1.278 | 0.751 |
| 131072 | 4.00 | 0.841 | 2.343 |

Reducir el umbral a $1024$ baja la profundidad estimada, por lo que el
modelo le asigna la mayor aceleración. También genera $1024$ hojas y
diez niveles de concatenación. Con umbral $131072$ se generan ocho hojas
de $125\,000$ elementos y tres niveles de concatenación. Esta reducción
en tareas y copias puede ayudar a explicar sus $29.227$ ms en la corrida
con heap fijo. Con `test run`, el umbral $8192$ obtuvo el menor tiempo
en ambos tamaños. Con $250\,000$ partículas y heap fijo, los umbrales
$1024$ y $131072$ quedaron a $0.064$ ms entre sí.

### Fila de control

Para $250\,000$ partículas, el control fue $0.878$ con `test run` y
$1.025$ con heap fijo. Para un millón, fue $0.320$ y $0.714$.
La desviación respecto a $1$ en el sistema mayor limita la precisión con
que se puede atribuir la aceleración al reparto. La recolección de basura,
la presión de memoria y la actividad del sistema son factores posibles.
Los tiempos registrados describen estas corridas y el umbral más rápido
cambió entre ellas.

### Fracción secuencial efectiva

La ley de Amdahl relaciona la aceleración $S$ con una fracción secuencial efectiva $f$ en $p$ procesadores:

$$
S=\frac{1}{f+(1-f)/p},
\qquad
f=\frac{1/S-1/p}{1-1/p}.
$$

Con $p=8$ y $S=2.343$, resulta $f\approx0.345$, cerca del $35\,\%$. Con $S=1.313$ de la otra corrida, resulta $f\approx0.728$. Estas cifras incluyen el efecto de las sobrecargas y son sensibles al control. Con la primera estimación, el límite ideal sería $1/f\approx2.90$. Agregar núcleos reduce el término paralelo $(1-f)/p$ y deja un coste residual. En esta implementación contribuyen a ese coste la concatenación final, la coordinación de tareas y la saturación de memoria.

