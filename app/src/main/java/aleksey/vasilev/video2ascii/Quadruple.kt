package aleksey.vasilev.video2ascii

import kotlin.math.sqrt
import kotlin.math.pow

/** @param nw - average pixels value in grayscale, north-west corner
 *  @param ne - average pixels value in grayscale, north-east corner
 *  @param sw - average pixels value in grayscale, south-west corner
 *  @param se - average pixels value in grayscale, south-east corner
 *  @return Quadruple to count distance between initial IntArray and possible letter
 * */
data class Quadruple(val nw: Int, val ne: Int, val sw: Int, val se: Int) {

    /** @param other - other Quadruple to count distance between
     *  @return distance
     * */
    fun getDistance(other: Quadruple): Float {
        return sqrt(
            (nw.toFloat() - other.nw).pow(2f) +
                    (ne.toFloat() - other.ne).pow(2f) +
                    (sw.toFloat() - other.sw).pow(2f) +
                    (se.toFloat() - other.se).pow(2f)
        )
    }
}
