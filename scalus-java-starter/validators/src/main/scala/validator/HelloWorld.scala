package validator

import scalus.Compiler.compile
import scalus.*
import scalus.ledger.api.PlutusLedgerLanguage
import scalus.uplc.Constant.Data
import scalus.utils.Utils

enum Redeemer:
  case Accept
  case Reject

object HelloWorld {
//  val validator = compile {
//    (datum: Datum, redeemer: Data, context: Data) =>
//      datum match
//        case Datum.Accept => true
//        case Datum.Reject => false
//  }

  val validator = compile:
    (redeemer: Redeemer, context: Data) =>
      redeemer match
        case Redeemer.Accept => true
        case Redeemer.Reject => false


  @main def main(): Unit = {
    val str = validator.toUplc().pretty.render(80)
    val cbor = validator.doubleCborHex(version = (1, 0, 0))
    val program = validator.toPlutusProgram(version = (1, 0, 0))

    println(str)
    Utils.writePlutusFile("helloworld.plutus", program, PlutusLedgerLanguage.PlutusV2)
  }

}
