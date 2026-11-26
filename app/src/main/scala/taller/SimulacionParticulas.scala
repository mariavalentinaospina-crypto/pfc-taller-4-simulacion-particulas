package taller

import common._
import org.scalameter._
import scala.annotation.tailrec

/**
 * Taller 4 — simulación de partículas repartida entre varios núcleos.
 *
 * Complete cada función. Las firmas y los tipos no se modifican.
 *
 * Las abstracciones `parallel` y `task` vienen en el paquete `common`, que ya
 * está en el proyecto.
 */
class SimulacionParticulas {

  type Sistema = Array[Particula]

  // Punto 1: un paso de la simulación -----------------------------------------

  /** La posición nueva es la anterior más la velocidad por dt. */
  def mover(p: Particula, dt: Double): Particula =
    p.copy(pos = p.pos + p.vel * dt)

  /** Avanza todo el sistema un paso de tiempo dt. */
  def pasoSecuencial(s: Sistema, dt: Double): Sistema =
    pasoSegmento(s, dt, 0, s.length)

  /** Avanza solo el tramo entre `desde` incluido y `hasta` excluido. */
  def pasoSegmento(s: Sistema, dt: Double, desde: Int, hasta: Int): Sistema =
    Array.tabulate(hasta - desde)(i => mover(s(desde + i), dt))

  /**
   * Parte el sistema por la mitad con `parallel` mientras el tramo tenga más
   * elementos que el umbral, y por debajo de esa cifra resuelve en secuencial.
   */
  def pasoParalelo(s: Sistema, dt: Double, umbral: Int): Sistema = {
    val limite = math.max(1, umbral)

    def avanzar(desde: Int, hasta: Int): Sistema = {
      val longitud = hasta - desde
      if (longitud <= limite) pasoSegmento(s, dt, desde, hasta)
      else {
        val mitad = desde + longitud / 2
        val (izquierda, derecha) = parallel(avanzar(desde, mitad), avanzar(mitad, hasta))
        Array.concat(izquierda, derecha)
      }
    }

    if (s.length <= limite) pasoSecuencial(s, dt)
    else avanzar(0, s.length)
  }

  // Punto 2: magnitudes globales ----------------------------------------------

  /** La suma de las energías cinéticas, media masa por velocidad al cuadrado. */
  def energiaSecuencial(s: Sistema): Double =
    energiaParalela(s, s.length)

  /** La suma de las masas. */
  def masaTotal(s: Sistema): Double =
    s.foldLeft(0.0)((masa, p) => masa + p.masa)

  /**
   * La suma de las posiciones ponderadas por la masa, dividida entre la masa
   * total. Sin masa, el origen.
   */
  def centroDeMasaSecuencial(s: Sistema): Vector2 =
    centroDeMasaParalelo(s, s.length)

  // Punto 3: las mismas magnitudes en paralelo --------------------------------

  /** La energía del sistema, repartida con el mismo umbral del punto 1. */
  def energiaParalela(s: Sistema, umbral: Int): Double = {
    val limite = math.max(1, umbral)

    @tailrec
    def sumar(i: Int, hasta: Int, energia: Double): Double =
      if (i == hasta) energia
      else {
        val p = s(i)
        val rapidezCuadrada = p.vel.x * p.vel.x + p.vel.y * p.vel.y
        sumar(i + 1, hasta, energia + 0.5 * p.masa * rapidezCuadrada)
      }

    def reducir(desde: Int, hasta: Int): Double = {
      val longitud = hasta - desde
      if (longitud <= limite) sumar(desde, hasta, 0.0)
      else {
        val mitad = desde + longitud / 2
        val (izquierda, derecha) = parallel(reducir(desde, mitad), reducir(mitad, hasta))
        izquierda + derecha
      }
    }

    reducir(0, s.length)
  }

  /**
   * El centro de masa, repartido. Promediar los promedios de cada mitad da
   * otra cosa cuando las dos mitades no pesan igual.
   */
  def centroDeMasaParalelo(s: Sistema, umbral: Int): Vector2 = {
    val limite = math.max(1, umbral)

    @tailrec
    def sumar(i: Int, hasta: Int, momento: Vector2, masa: Double): (Vector2, Double) =
      if (i == hasta) (momento, masa)
      else {
        val p = s(i)
        sumar(i + 1, hasta, momento + p.pos * p.masa, masa + p.masa)
      }

    def reducir(desde: Int, hasta: Int): (Vector2, Double) = {
      val longitud = hasta - desde
      if (longitud <= limite) sumar(desde, hasta, Vector2(0, 0), 0.0)
      else {
        val mitad = desde + longitud / 2
        val ((momentoIzq, masaIzq), (momentoDer, masaDer)) =
          parallel(reducir(desde, mitad), reducir(mitad, hasta))
        (momentoIzq + momentoDer, masaIzq + masaDer)
      }
    }

    val (momento, masa) = reducir(0, s.length)
    if (masa == 0.0) Vector2(0, 0)
    else Vector2(momento.x / masa, momento.y / masa)
  }

  // Punto 4: trabajo y profundidad --------------------------------------------

  /** El número total de operaciones, una por partícula. */
  def trabajo(n: Int): Int = n

  /** Cuántas veces se parte a la mitad, más el tramo que queda sin partir. */
  def profundidad(n: Int, umbral: Int): Int = {
    val limite = math.max(1, umbral)

    def porNivel(menor: Int, mayor: Int, nivel: Int): Int =
      if (mayor <= limite) nivel + mayor
      else if (menor <= limite)
        nivel + math.max(menor, 1 + (mayor - mayor / 2))
      else porNivel(menor / 2, mayor - mayor / 2, nivel + 1)

    porNivel(n, n, 0)
  }

  /** La profundidad más el trabajo entre p procesadores. */
  def tiempoEstimado(n: Int, p: Int, umbral: Int): Double =
    profundidad(n, umbral) + trabajo(n).toDouble / p

  // Punto 5: medir la aceleración ---------------------------------------------

  /** Mediana de varias mediciones de ScalaMeter, en milisegundos. */
  def medir[A](repeticiones: Int)(cuerpo: => A): Double = {
    val muestras = Vector.fill(repeticiones) {
      (withWarmer(new Warmer.Default) measure { cuerpo }).value
    }
    muestras.sorted.apply(muestras.length / 2)
  }

  /** El cociente entre el tiempo secuencial y el paralelo. */
  def aceleracion(tSecuencial: Double, tParalelo: Double): Double =
    tSecuencial / tParalelo

  /**
   * El umbral que menos tarda de una lista de candidatos. Con la lista vacía,
   * el tamaño del sistema, que equivale a no repartir.
   */
  def umbralConveniente(s: Sistema, dt: Double, umbrales: List[Int]): Int =
    if (umbrales.isEmpty) s.length
    else umbrales.minBy(umbral => medir(3)(pasoParalelo(s, dt, umbral)))
}
