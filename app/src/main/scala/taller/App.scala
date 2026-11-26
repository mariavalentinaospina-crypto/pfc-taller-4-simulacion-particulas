package taller

import java.util.Locale

/**
 * El punto 5 pide medir aquí, sobre un sistema de al menos un millón de
 * partículas, el tiempo de `pasoSecuencial` y el de `pasoParalelo` con varios
 * umbrales, y reportar la aceleración de cada uno.
 *
 * Lo que hay ahora es el arranque mínimo sobre dos partículas.
 */
object App {

  def main(args: Array[String]): Unit = {
    val sim = new SimulacionParticulas()
    import sim._

    val tamanos = List(250000, 1000000)
    val procesadores = Runtime.getRuntime.availableProcessors()
    val repeticiones = 7
    val dt = 0.5

    println(s"Procesadores logicos disponibles: $procesadores")
    println(s"ScalaMeter: mediana de $repeticiones mediciones por variante")
    println("| n | Umbral | Secuencial (ms) | Paralelo (ms) | Aceleracion | Profundidad | Tiempo estimado |")
    println("| ---: | ---: | ---: | ---: | ---: | ---: | ---: |")

    tamanos.foreach { n =>
      val sistema: Sistema = Array.tabulate(n) { i =>
        Particula(Vector2(i % 1000, i / 1000), Vector2(i % 7 - 3, i % 5 - 2), 1.0 + i % 11)
      }
      val umbrales = List(n, 1024, 8192, 32768, 131072)
      val control = Vector.tabulate(repeticiones) { i =>
        if (i % 2 == 0) {
          val secuencial = medir(1)(pasoSecuencial(sistema, dt))
          (secuencial, medir(1)(pasoParalelo(sistema, dt, n)))
        } else {
          val paralelo = medir(1)(pasoParalelo(sistema, dt, n))
          (medir(1)(pasoSecuencial(sistema, dt)), paralelo)
        }
      }
      val secuencial = control.map(_._1).sorted.apply(repeticiones / 2)
      val sinReparto = control.map(_._2).sorted.apply(repeticiones / 2)

      umbrales.foreach { umbral =>
        val paralelo = if (umbral == n) sinReparto
          else medir(repeticiones)(pasoParalelo(sistema, dt, umbral))
        val aceleracionMedida = aceleracion(secuencial, paralelo)
        val estimado = tiempoEstimado(n, procesadores, umbral)
        val fila = s"| $n | $umbral | %.3f | %.3f | " +
          s"%.3f | ${profundidad(n, umbral)} | %.2f |"
        println(String.format(Locale.ROOT, fila,
          Double.box(secuencial), Double.box(paralelo), Double.box(aceleracionMedida), Double.box(estimado)))
      }
    }
  }
}
