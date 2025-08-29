package Ex0
import chisel3._
import chisel3.util.Counter
import chisel3.experimental.MultiIOModule

class MatMul(val rowDimsA: Int, val colDimsA: Int) extends MultiIOModule {
  val io = IO(
    new Bundle {
      val dataInA = Input(UInt(32.W))
      val dataInB = Input(UInt(32.W))
      val dataOut = Output(UInt(32.W))
      val outputValid = Output(Bool())
    }
  )
  
  val debug = IO(
    new Bundle {
      val myDebugSignal = Output(Bool())
    }
  )

  // Matrix modules
  val matrixA = Module(new Matrix(rowDimsA, colDimsA)).io
  val matrixB = Module(new Matrix(rowDimsA, colDimsA)).io
  val dotProdCalc = Module(new DotProd(colDimsA)).io

  // Integrated control logic (previously in Control class)
  val setupCounter = Counter(rowDimsA * colDimsA)
  val inExecutionModeRegister = RegInit(false.B)
  val inExecutionMode = inExecutionModeRegister

  // Setup phase row/col indices
  val setupRowIdx = setupCounter.value / colDimsA.U
  val setupColIdx = setupCounter.value % colDimsA.U

  // Execution phase counters
  val rowA = RegInit(UInt(32.W), 0.U)
  val rowB = RegInit(UInt(32.W), 0.U)
  val col = RegInit(UInt(32.W), 0.U)

  // Control logic
  when (setupCounter.inc()) {
    inExecutionModeRegister := true.B
  }

  // Wire the outputs
  io.dataOut := dotProdCalc.dataOut
  when (inExecutionMode && dotProdCalc.outputValid) {
    printf("TRUE: %d\n", dotProdCalc.dataOut)
    io.outputValid := true.B
  } otherwise {
    io.outputValid := false.B
  }

  // Setup mode
  matrixA.dataIn := io.dataInA
  matrixB.dataIn := io.dataInB
  matrixA.rowIdx := setupRowIdx
  matrixA.colIdx := setupColIdx
  matrixB.rowIdx := setupRowIdx
  matrixB.colIdx := setupColIdx
  matrixA.writeEnable := !inExecutionMode
  matrixB.writeEnable := !inExecutionMode

  // Execution mode
  dotProdCalc.dataInA := matrixA.dataOut
  dotProdCalc.dataInB := matrixB.dataOut
  when (inExecutionMode) {
    matrixA.rowIdx := rowA
    matrixB.rowIdx := rowB
    matrixB.colIdx := col
    matrixA.colIdx := col
    
    col := col + 1.U
    when (col === colDimsA.U - 1.U) {
      col := 0.U
      rowB := rowB + 1.U
      when (rowB === rowDimsA.U - 1.U) {
        rowB := 0.U
        rowA := rowA + 1.U
      }
    }
    printf("%d %d %d\n", rowA, rowB, col)
  }

  debug.myDebugSignal := false.B
}