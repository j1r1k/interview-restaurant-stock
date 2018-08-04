package net.marsicek.restaurant

import org.scalatest.{Assertion, Assertions}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

trait AwaitAsyncAssertion extends Assertions {

  /**
    * Scalacheck doesn't support asynchronous property checking.
    * This is quick and dirty workaround to have the functionality available.
    *
    * See: https://github.com/rickynils/scalacheck/issues/214
    *
    * @param assertion predicate that must hold
    * @param futureValue value to be awaited that is plugged into assertion
    * @param timeout maximum wait time for result of futureValue
    * @return assertion checking the predicate on result of futureValue
    */
  def asyncAssert[A](assertion: A => Boolean)(futureValue: Future[A])
                    (implicit ec: ExecutionContext, timeout: Duration = 1 second): Assertion =
    assert(assertion(Await.result(futureValue, timeout)))

}
