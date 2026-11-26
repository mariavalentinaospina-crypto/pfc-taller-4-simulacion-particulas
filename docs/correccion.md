# Informe de corrección

## Avance de partículas

Para una partícula $p=(r,v,m)$, `mover` construye $f(p)=(r+dt\,v,v,m)$. La copia conserva $v$ y $m$. Para $0\le a\le b\le n$, `pasoSegmento` construye una salida de longitud $b-a$ y su celda $j$ contiene $f(s_{a+j})$. En particular, `pasoSecuencial` obtiene $f(s_i)$ para cada índice $i$ de $[0,n)$.

Sea $u=\max(1,\text{umbral})$. Probamos por inducción en $b-a$ que la llamada paralela produce el mismo tramo. Si $b-a\le u$, llama a `pasoSegmento` (la llamada inicial puede pasar por `pasoSecuencial`). Si $b-a>u$, el corte $c=a+\lfloor(b-a)/2\rfloor$ cumple $a\lt c\lt b$. Los dos tramos son más cortos y valen las hipótesis de inducción. Además,

```math
[a,c)\cap[c,b)=\varnothing,\qquad [a,c)\cup[c,b)=[a,b).
```

La concatenación de ambas salidas en orden conserva todos los índices, una sola vez, y vale para cualquier agrupación porque $(X\mathbin{++}Y)\mathbin{++}Z=X\mathbin{++}(Y\mathbin{++}Z)$. El arreglo vacío es la identidad de la concatenación. Los casos base cubren también los umbrales no positivos gracias al límite efectivo.

## Magnitudes del sistema

`masaTotal` comienza con cero y suma $m_i$ en cada paso. Tras procesar $k$ partículas tiene $\sum_{i=0}^{k-1}m_i$. En una hoja $[a,b)$, al entrar a `sumar` con índice $i$, cada acumulador contiene la suma de los aportes de $[a,i)$. El caso $i=a$ cumple el invariante por ser una suma vacía. El paso recursivo agrega el aporte del índice $i$ y avanza a $i+1$. Al alcanzar $i=b$, el acumulador cubre exactamente la hoja. Los aportes son

```math
e_i=\frac{1}{2}m_i(v_{x,i}^2+v_{y,i}^2),\qquad Q_i=(m_i r_i,m_i).
```

Por inducción sobre las divisiones, cada rama de energía devuelve $\sum_{i=a}^{b-1}e_i$ y cada rama de centro de masa devuelve $\sum_{i=a}^{b-1}Q_i$. En la hoja esto se obtiene por el invariante. Para una partición se suman los dos resultados sobre intervalos disjuntos. La suma escalar y la suma componente a componente $(w_1,M_1)\oplus(w_2,M_2)=(w_1+w_2,M_1+M_2)$ son asociativas sobre los reales y tienen identidad cero. El recorrido secuencial ejecuta la misma reducción sobre el tramo completo.

El centro es $w/M$ si $M\ne0$ y el origen en caso contrario. Por ejemplo, dos mitades con centros en $x=0$ y $x=8$ y masas $1$ y $3$ tienen centro en $x=6$. Promediar sus centros daría $4$. La masa de cada rama se conserva junto al momento hasta la división final.

El promedio binario $A(x,y)=(x+y)/2$ tampoco es asociativo. Con $0$, $4$
y $8$, se obtiene $A(A(0,4),8)=5$ y $A(0,A(4,8))=3$. Conservar la suma
ponderada y la masa permite agrupar los tramos de cualquier forma.

En `Double`, los redondeos de las sumas dependen del orden de acumulación. Las pruebas de estas magnitudes comparan con tolerancia.

## Trabajo y profundidad

Cada índice requiere un movimiento, así que el trabajo del modelo es $W(n)=n$. Una hoja de longitud $k$ en el nivel $d$ aporta $d+k$ a la profundidad. Por tanto,

```math
D(n,u)=
\begin{cases}
n & n\le u,\\
1+\max\{D(\lfloor n/2\rfloor,u),D(\lceil n/2\rceil,u)\} & n>u.
\end{cases}
```

Mientras todos los tramos de un nivel se dividen, sus longitudes difieren a lo sumo en uno. `profundidad` conserva los extremos de esas longitudes y el nivel. Al dividir todos los tramos, el nuevo mínimo es el piso de la mitad del anterior y el nuevo máximo es el techo de la mitad del anterior. Si todos son hojas, gana la longitud máxima. Si hay hojas y tramos por dividir en el mismo nivel, sus tamaños son $u$ y $u+1$. Basta comparar $d+u$ con $d+1+\lceil(u+1)/2\rceil$, porque todos los hijos ya son hojas. El cálculo visita a lo sumo un estado por nivel. Para $p>0$, `tiempoEstimado` usa $D+W/p$ con división real.

## Aceleración y selección

Para tiempos positivos, $S=T_{\rm sec}/T_{\rm par}$. El resultado es $1$ si ambos tiempos coinciden. `umbralConveniente` aplica `minBy` a las medianas medidas y, para una lista no vacía, devuelve un candidato que alcanza el mínimo observado. El caso vacío devuelve $n$, que deja el sistema en un único tramo. Como las medianas dependen de la ejecución, las pruebas no exigen que gane un umbral fijo.

## Verificación con pruebas

La suite reúne 36 pruebas del taller y 25 casos propios, cinco por punto.
Los casos propios comprueban, entre otros, el orden del avance con una
longitud impar, el centro de masa con mitades de distinto peso, el coste
de una rama que termina antes y el cociente fraccionario de la estimación.
También comprueban que buscar un umbral conserva el sistema de entrada.
Gradle ejecutó los 61 casos sin fallos.
