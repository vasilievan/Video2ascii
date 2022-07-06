package aleksey.vasilev.video2ascii

import kotlin.math.sqrt
import kotlin.math.pow

data class Quadruple(val nw: Int, val ne: Int, val sw: Int, val se: Int) {
    fun getDistance(other: Quadruple): Float {
        return sqrt(
            (nw.toFloat() - other.nw).pow(2f) +
                    (ne.toFloat() - other.ne).pow(2f) +
                    (sw.toFloat() - other.sw).pow(2f) +
                    (se.toFloat() - other.se).pow(2f)
        )
    }
}
