package net.marsicek.restaurant

import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Arbitrary.arbitrary

trait StockItemGenerators {

  implicit def genToArbitrary[A : Gen]: Arbitrary[A] = Arbitrary(implicitly[Gen[A]])

  /**
    * There are some sane limits to generator sizes in order to prevent too large examples
    */

  implicit lazy val genStockItemId: Gen[StockItemId] =
    Gen.listOfN(12, Gen.alphaNumChar).map(_.toString).map(StockItemId.apply)

  implicit lazy val genStockItem: Gen[StockItem] = for {
    id <- arbitrary[StockItemId]
    name <- Gen.resize(16, Gen.alphaStr)
    quantity <- Gen.posNum[Double]
  } yield StockItem(id, name, quantity)

  implicit lazy val genStockItemUpdate: Gen[StockItemUpdate] = Gen.posNum[Double].map(UpdateQuantity.apply)
}
