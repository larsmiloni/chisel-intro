package Ex0

import chisel3._
import chisel3.util.Counter

class DotProd(val elements: Int) extends Module {

  val io = IO(
    new Bundle {
      val dataInA     = Input(UInt(32.W))
      val dataInB     = Input(UInt(32.W))

      val dataOut     = Output(UInt(32.W))
      val outputValid = Output(Bool())
    }
  )

  val product = io.dataInA * io.dataInB
  val accumulator = RegInit(0.U(32.W))
  val (count, wrap) = Counter(true.B, elements)
  val nextSum = accumulator + product

  accumulator := nextSum
  io.dataOut := nextSum
  io.outputValid := wrap

  when(wrap) {
    accumulator := 0.U
  }
}
