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
            int opcode = (int) (IR / 10000);
            int Op1Mode = (int) ((IR % 10000) / 1000);
            int Op1GPR = (int) (((IR % 10000) % 1000) / 100);
            int Op2Mode = (int) ((((IR % 10000) % 1000) % 100) / 10);
            int Op2GPR = (int) ((((IR % 10000) % 1000) % 100) % 10);
            switch (opcode) {
                case 0: // Halt
                    isToExecute = false;
                    System.out.println("Program Halted");
                    break;
                case 1: // Add
                    executeArithmetic('+');
                    break;
                case 2: // Subtract
                    executeArithmetic('-');
                    break;
                case 3: // Multiply
                    executeArithmetic('*');
                    break;
                case 4: // Divide
                    executeArithmetic('/');
                    break;
                case 5: // Move
                    executeMove();
                    break;
                case 6: // Branch
                    executeBranch();
                    break;
                case 7: // BrOnMinus
                    executeBranchOnCondition(value -> value < 0);
                    break;
                case 8: // BrOnPlus
                    executeBranchOnCondition(value -> value > 0);
                    break;
                case 9: // BrOnZero
                    executeBranchOnCondition(value -> value == 0);
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

    private static void executeArithmetic(char operation ) {
        long op1Addr = memory[(int) PC++];
        long op2Addr = memory[(int) PC++];
    
        if (op1Addr >= MEMORY_SIZE || op2Addr >= MEMORY_SIZE) {
            System.err.println("Invalid Memory Address");
            return;
        }
        long op1 = memory[(int) op1Addr];
        long op2 = memory[(int) op2Addr];
        switch (operation) {
            case '+': memory[(int) op1Addr] = op1 + op2; break;
            case '-': memory[(int) op1Addr] = op1 - op2; break;
            case '*': memory[(int) op1Addr] = op1 * op2; break;
            case '/': memory[(int) op1Addr] = (op2 != 0) ? op1 / op2 : 0;
        }
    }

    private static void executeMove() {
        long destAddr = memory[(int) PC++];
        long srcAddr = memory[(int) PC++];
        if (destAddr < MEMORY_SIZE && srcAddr < MEMORY_SIZE) {
            memory[(int) destAddr] = memory[(int) srcAddr];
        } else {
            System.err.println("Invalid Memory Address");
        }
    }
    
    private static void executeBranch() {
        long branchAddr = memory[(int) PC++];
        if (branchAddr >= 0 && branchAddr < MEMORY_SIZE) {
            PC = branchAddr;
        } else {
            System.err.println("Invalid Branch Address");
        }
    }

    private static void executeBranchOnCondition(java.util.function.LongPredicate condition) {
        long op1Addr = memory[(int) PC++];
        long branchAddr = memory[(int) PC++];
        if (op1Addr >= MEMORY_SIZE || branchAddr >= MEMORY_SIZE) {
            System.err.println("Invalid Memory Address");
            return;
        }
        long op1 = memory[(int) op1Addr];
        if (condition.test(op1)) {
            PC = branchAddr;
        }
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

    private static long fetchOperand(long OpMode, long OpGPR) {
        long OpAddress;
        long OpValue = 0;

        if(((int) OpGPR) > 8 || ((int) OpGPR) < 0) {
            System.out.println("Invalid GPR Address");
            return -2;
        }

        switch((int)OpMode) {
            case 1: // Register Mode
                OpValue = GPRs[(int)OpGPR];
                break;
            case 2: // Register Deferred Mode
                OpAddress = GPRs[(int) OpGPR];
                if(((int) OpAddress) < 10000 && ((int) OpAddress) >= 0 ) {
                    OpValue = memory[(int) OpAddress];
                } else {
                    System.out.println("Invalid Memory Address");
                    return -2;
                }
                break;
            case 3: // Autoincrement mode
                OpAddress = GPRs[(int) OpGPR];
                if(((int) OpAddress) < 10000 && ((int) OpAddress) >= 0 ) {
                    OpValue = memory[(int) OpAddress];
                } else {
                    System.out.println("Invalid Memory Address");
                    return -2;
                }
                GPRs[(int) OpGPR]++;
                break;
            case 4: // Autodecrement mode
                OpAddress = --GPRs[(int) OpGPR];
                if(((int) OpAddress) < 10000 && ((int) OpAddress) >= 0 ) {
                    OpValue = memory[(int) OpAddress];
                } else {
                    System.out.println("Invalid Memory Address");
                    return -2;
                }
                break;
            case 5: // Direct Mode
                OpAddress = memory[(int)PC++];
                if(((int) OpAddress) < 10000 && ((int) OpAddress) >= 0 ) {
                    OpValue = memory[(int) OpAddress];
                } else {
                    System.out.println("Invalid Memory Address");
                    return -2;
                }
                break;
            case 6: // Immediate Mode
                OpValue = memory[(int) PC++];
                break;
            default: // Invalid Mode
                System.out.println("Invalid Operand Mode");
                return -3;
        }

        return OpValue;
    }
}
