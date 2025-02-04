import java.io.*;
import java.util.*;

public class HypoMachine {
    // Memory and Registers
    private static final int MEMORY_SIZE = 10000;
    private static long[] memory = new long[MEMORY_SIZE];
    private static long[] GPRs = new long[8]; // General-purpose registers
    private static long MAR, MBR, IR, SP, PC, PSR, Clock;

    public static void main(String[] args) {
        initializeSystem();
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter executable file name: ");
        String fileName = scanner.nextLine();
        long startAddress = absoluteLoader(fileName);
        if (startAddress >= 0) {
            PC = startAddress;
            dumpMemory("After Loading Program", 0, 100);
            executeProgram();
            dumpMemory("After Executing Program", 0, 100);
        }
        scanner.close();
    }

    private static void initializeSystem() {
        Arrays.fill(memory, 0);
        Arrays.fill(GPRs, 0);
        MAR = MBR = IR = SP = PC = PSR = Clock = 0;
    }

    private static long absoluteLoader(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 2) {
                    long address = Long.parseLong(parts[0]);
                    long value = Long.parseLong(parts[1]);
                    if (address < 0) return value; // End of program, return PC start
                    if (address >= MEMORY_SIZE) return -1; // Invalid address
                    memory[(int) address] = value;
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading file: " + e.getMessage());
            return -1;
        }
        return -1; // Error if no end marker
    }

    private static void executeProgram() {
        boolean isToExecute = true;
        while (isToExecute) {
            if (PC < 0 || PC >= MEMORY_SIZE) {
                System.err.println("Runtime Error: Invalid PC Address");
                break;
            }
            MAR = PC;
            MBR = memory[(int) MAR];
            IR = MBR;
            PC++;
            long opcode = (IR / 10000);
            long Op1Mode = ((IR % 10000) / 1000);
            long Op1GPR = (((IR % 10000) % 1000) / 100);
            long Op2Mode = ((((IR % 10000) % 1000) % 100) / 10);
            long Op2GPR = ((((IR % 10000) % 1000) % 100) % 10);
            switch ((int)opcode) {
                case 0: // Halt
                    isToExecute = false;
                    System.out.println("Program Halted");
                    break;
                case 1: // Add
                    executeArithmetic('+', Op1Mode, Op1GPR, Op2Mode, Op2GPR);
                    break;
                case 2: // Subtract
                    executeArithmetic('-', Op1Mode, Op1GPR, Op2Mode, Op2GPR);
                    break;
                case 3: // Multiply
                    executeArithmetic('*', Op1Mode, Op1GPR, Op2Mode, Op2GPR);
                    break;
                case 4: // Divide
                    executeArithmetic('/', Op1Mode, Op1GPR, Op2Mode, Op2GPR);
                    break;
                case 5: // Move
                    executeMove(Op1Mode, Op1GPR, Op2Mode, Op2GPR);
                    break;
                case 6: // Branch
                    executeBranch();
                    break;
                case 7: // BrOnMinus
                    executeBranchOnCondition(Op1Mode, Op1GPR, value -> value < 0);
                    break;
                case 8: // BrOnPlus
                    executeBranchOnCondition(Op1Mode, Op1GPR, value -> value > 0);
                    break;
                case 9: // BrOnZero
                    executeBranchOnCondition(Op1Mode, Op1GPR, value -> value == 0);
                    break;
                case 10: // Push
                    executePush();
                    break;
                case 11: //Pop
                    executePop();
                    break;
                default:
                    System.err.println("Unknown Opcode: " + opcode);
                    isToExecute = false;
            }
            Clock++;
        }
    }

    private static int executeArithmetic( char operator, long Op1Mode, long Op1GPR, long Op2Mode, long Op2GPR ) {
        

        long Op1Value = fetchOperand(Op1Mode, Op1GPR)[1];
        long Op2Value = fetchOperand(Op2Mode, Op2GPR)[1];
        long Op1Address = fetchOperand(Op1Mode, Op1GPR)[0];
        long result;

        if(Op2Value == 0 && operator == '/') {
            System.out.println("Invalid operation: Division by Zero");
            return -4;
        }

        switch (operator) {
            case '+': result = Op1Value + Op2Value; break;
            case '-': result = Op1Value - Op2Value; break;
            case '*': result = Op1Value * Op2Value; break;
            case '/': result = (Op2Value != 0) ? Op1Value / Op2Value : 0;
            default: 
                System.err.println("Invalid Operator");
                return -5;
        }

        if(Op1Mode == 1) {
            GPRs[(int)Op1GPR] = result;
        } else if (Op1Mode == 6) {
            System.err.println("Destination operand cannot be immediate value");
            return -6;
        } else {
            memory[(int) Op1Address] = result;
        }

        return 1;
    }

    private static int executeMove(long Op1Mode, long Op1GPR, long Op2Mode, long Op2GPR) {
        
        long Op2Value = fetchOperand(Op2Mode, Op2GPR)[1];
        long Op1Address = fetchOperand(Op1Mode, Op1GPR)[0];

        if(Op1Mode == 1) {
            GPRs[(int) Op1GPR] = Op2Value;
        } else if(Op1Mode == 6){
            System.err.println("Destination operand cannot be immediate value");
            return -6;
        } else {
            memory[(int)Op1Address] = Op2Value;
        }
        
        return 1;
    }
    
    private static void executeBranch() {
        long branchAddr = memory[(int) PC++];
        if (branchAddr >= 0 && branchAddr < MEMORY_SIZE) {
            PC = branchAddr;
        } else {
            System.err.println("Invalid Branch Address");
        }
    }

    private static int executeBranchOnCondition(long Op1Mode, long Op1GPR, java.util.function.LongPredicate condition) {
        long Op1Value = fetchOperand(Op1Mode, Op1GPR)[1];
        long Op1Address = fetchOperand(Op1Mode, Op1GPR)[0];
    
        long branchAddr = memory[(int) PC];
        if (Op1Address >= MEMORY_SIZE || branchAddr >= MEMORY_SIZE) {
            System.err.println("Invalid Memory Address");
            return -2;
        }
       
        if (condition.test(Op1Value)) {
            PC = branchAddr;
        } else {
            PC++;
        }

        return 1;
    }

    private static void executePush() {
        long address = memory[(int) PC];
        PC++;
        if(address >= MEMORY_SIZE) {
            System.err.println("Invalid: Memory Address exceeds the memory size");
        }
        if(SP <= 0) {
            System.err.println("Stack Overflow");
            return;
        }
        int addressToGetValueFrom = (int) address;
        int stackAddressToPushValueTo = (int) SP;
        memory[stackAddressToPushValueTo] = memory[addressToGetValueFrom];
        SP--;
    }

    private static void executePop() {
        long address = memory[(int) PC++];
        if(address >= MEMORY_SIZE) {
            System.err.println("Invalid: Memory Address exceeds the memory size");
        }
        int stackAddressToPopValueFrom = (int) SP;
        int addressToPutValueTo = (int) address;
        memory[addressToPutValueTo] = memory[stackAddressToPopValueFrom];
        SP++;
    }

    private static void dumpMemory(String message, int start, int size) {
        System.out.println(message);
        System.out.println("PC: " + PC + " | Clock: " + Clock);
        for (int i = start; i < start + size && i < MEMORY_SIZE; i += 10) {
            System.out.printf("%4d: ", i);
            for (int j = 0; j < 10 && (i + j) < MEMORY_SIZE; j++) {
                System.out.printf("%6d ", memory[i + j]);
            }
            System.out.println();
        }
    }

    private static long[] fetchOperand(long OpMode, long OpGPR) {
        long OpAddress;
        long OpValue;
        long[] operand = new long[2];

        if(((int) OpGPR) > 8 || ((int) OpGPR) < 0) {
            System.out.println("Invalid GPR Address");
            operand[0] = -2;
            operand[1] = -2;
            return operand;
        }

        switch((int)OpMode) {
            case 1: // Register Mode
                OpValue = GPRs[(int)OpGPR];
                OpAddress = -1;
                break;
            case 2: // Register Deferred Mode
                OpAddress = GPRs[(int) OpGPR];
                if(((int) OpAddress) < 10000 && ((int) OpAddress) >= 0 ) {
                    OpValue = memory[(int) OpAddress];
                } else {
                    System.out.println("Invalid Memory Address");
                    operand[0] = -2;
                    operand[1] = -2;
                    return operand;
                }
                break;
            case 3: // Autoincrement mode
                OpAddress = GPRs[(int) OpGPR];
                if(((int) OpAddress) < 10000 && ((int) OpAddress) >= 0 ) {
                    OpValue = memory[(int) OpAddress];
                } else {
                    System.out.println("Invalid Memory Address");
                    operand[0] = -2;
                    operand[1] = -2;
                    return operand;
                }
                GPRs[(int) OpGPR]++;
                break;
            case 4: // Autodecrement mode
                OpAddress = --GPRs[(int) OpGPR];
                if(((int) OpAddress) < 10000 && ((int) OpAddress) >= 0 ) {
                    OpValue = memory[(int) OpAddress];
                } else {
                    System.out.println("Invalid Memory Address");
                    operand[0] = -2;
                    operand[1] = -2;
                    return operand;
                }
                break;
            case 5: // Direct Mode
                OpAddress = memory[(int)PC++];
                if(((int) OpAddress) < 10000 && ((int) OpAddress) >= 0 ) {
                    OpValue = memory[(int) OpAddress];
                } else {
                    System.out.println("Invalid Memory Address");
                    operand[0] = -2;
                    operand[1] = -2;
                    return operand;
                }
                break;
            case 6: // Immediate Mode
                OpValue = memory[(int) PC++];
                OpAddress = -1;
                break;
            default: // Invalid Mode
                System.out.println("Invalid Operand Mode");
                operand[0] = -3;
                operand[1] = -3;
                return operand;
        }

        operand[0] = OpAddress;
        operand[1] = OpValue;
        return operand;
    }
}
