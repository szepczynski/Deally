package pl.deally {
package model {

import net.liftweb.mapper._

object DealTag extends DealTag with LongKeyedMetaMapper[DealTag]

class DealTag extends LongKeyedMapper[DealTag] with IdPK {

  def getSingleton = DealTag
  
  object dealid extends LongMappedMapper(this, Deal)
  object tagid extends LongMappedMapper(this, Tag)

}


}
}
