package net.marsicek.restaurant

import io.circe.parser.{decode, _}
import io.circe.syntax._
import org.scalatest.FunSpec
import org.scalatest.prop.PropertyChecks

class StockItemSpec extends FunSpec with PropertyChecks with StockItemGenerators {

  describe("StockItem") {
    describe("properties") {
      it("encoding and decoding should be isomorphic") {
        forAll { item: StockItem =>
          decode[StockItem](item.asJson.toString()) match {
            case Right(decodedItem) => assert(decodedItem == item)
            case Left(error) => fail(error)
          }
        }
      }
    }

    it("should be possible encode StockItem to JSON") {
      val item = StockItem(StockItemId("a1"), "Apple", 10)

      val expected =
        parse("""{ "id": "a1", "name": "Apple", "quantity": 10.0 }""").right.get

      assert(item.asJson == expected)
    }
  }

}
