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
            
        } 
        dumpMemory("After Executing Program", 0, 100);
        scanner.close();
    }

    private static void initializeSystem() {
        Arrays.fill(memory, 0);
        Arrays.fill(GPRs, 0);
        MAR = MBR = IR = PC = PSR = Clock = 0;
        SP = 9999;
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
                    if (address >= MEMORY_SIZE) {
                        PSR = -1;
                        System.err.println("Error: Invalid Instruction Address");
                        return -1; // Invalid address
                    } 
                    memory[(int) address] = value;
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading file: " + e.getMessage());
            PSR = -1;
            return -1;
        }
        PSR = -1;
        System.err.println("Error: No End Marker Found");
        return -1; // Error if no end marker
    }

    private static void executeProgram() {
        boolean isToExecute = true;
        while (isToExecute) {
            if (PC < 0 || PC >= MEMORY_SIZE) {
                System.err.println("Error: Invalid PC Address");
                break;
            }
            System.out.println("You are in the ExecuteProgram(). Next line of instruction.");
            MAR = PC;
            MBR = memory[(int) MAR];
            IR = MBR;
            PC++;
            long opcode = (IR / 10000);
            long Op1Mode = ((IR % 10000) / 1000);
            long Op1GPR = (((IR % 10000) % 1000) / 100);
            long Op2Mode = ((((IR % 10000) % 1000) % 100) / 10);
            long Op2GPR = ((((IR % 10000) % 1000) % 100) % 10);
            System.out.println("opcode: " + opcode);
            System.out.println("Op1Mode: " + Op1Mode);
            System.out.println("Op1GPR: " + Op1GPR);
            System.out.println("Op2Mode: " + Op2Mode);
            System.out.println("Op2GPR: " + Op2GPR);
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
                    PSR = -1;
                    isToExecute = false;
            }
            Clock++;
        }
    }

    private static void executeArithmetic( char operator, long Op1Mode, long Op1GPR, long Op2Mode, long Op2GPR ) {
        
        // if(fetchOperand(Op1Mode, Op1GPR)[1] < 0 || fetchOperand(Op2Mode, Op2GPR)[1] < 0) {
        //     PSR = -1;
        //     System.out.println("Cant believe you are herererere");
        //     return;
        // }
        long[] operand1 = new long[2];
        long[] operand2 = new long[2];

        operand1 = fetchOperand(Op1Mode, Op1GPR);
        long Op1Value = operand1[1];
        long Op1Address = operand1[0];

        operand2 = fetchOperand(Op2Mode, Op2GPR);
        long Op2Value = operand2[1];
        
        long result;

        if(Op1Value < 0 || Op2Value < 0) {
            PSR = -1;
            System.out.println("You are in IF block. Your Op1Value or Op2Value are less than 0");
            return;
        }

        System.out.println("You are in executeArithmetic() after fetching operands and checking that they are > 0");

        if(Op2Value == 0 && operator == '/') {
            System.err.println("Error: Division by Zero");
            PSR = -1;
            return;
        }

        switch (operator) {
            case '+': result = Op1Value + Op2Value; break;
            case '-': result = Op1Value - Op2Value; break;
            case '*': result = Op1Value * Op2Value; break;
            case '/': result = (Op2Value != 0) ? Op1Value / Op2Value : 0;
            default: 
                System.err.println("Error: Invalid Operator");
                PSR = -1;
                return;
        }

        if(Op1Mode == 1) {
            GPRs[(int)Op1GPR] = result;
        } else if (Op1Mode == 6) {
            System.err.println("Destination operand cannot be immediate value");
            PSR = -1;
            return;
        } else {
            memory[(int) Op1Address] = result;
        }

      
    }

    private static void executeMove(long Op1Mode, long Op1GPR, long Op2Mode, long Op2GPR) {
        
        long Op2Value = fetchOperand(Op2Mode, Op2GPR)[1];
        long Op1Address = fetchOperand(Op1Mode, Op1GPR)[0];

        if(Op1Mode == 1) {
            GPRs[(int) Op1GPR] = Op2Value;
        } else if(Op1Mode == 6){
            System.err.println("Error: Destination operand cannot be immediate value");
            PSR = -1;
            return;
        } else {
            memory[(int)Op1Address] = Op2Value;
        } 
    }
    
    private static void executeBranch() {
        long branchAddr = memory[(int) PC++];
        if (branchAddr >= 0 && branchAddr < MEMORY_SIZE) {
            PC = branchAddr;
        } else {
            System.err.println("Error: Invalid Branch Address");
            PSR = -1;
        }
    }

    private static void executeBranchOnCondition(long Op1Mode, long Op1GPR, java.util.function.LongPredicate condition) {
        

        long Op1Value = fetchOperand(Op1Mode, Op1GPR)[1];
        // long Op1Address = fetchOperand(Op1Mode, Op1GPR)[0];

        System.out.println("Inside executeBranchOnCondition after fetching Op1Value");

    
        long branchAddr = memory[(int) PC];
        if (branchAddr >= MEMORY_SIZE) {
            System.err.println("Error: Invalid Memory Address");
            PSR = -1;
            return;
        }
       
        if (condition.test(Op1Value)) {
            PC = branchAddr;
        } else {
            PC++;
        }

        
    }

    private static void executePush() {
        long address = memory[(int) PC];
        PC++;
        if(address >= MEMORY_SIZE) {
            System.err.println("Error: Invalid Memory Address");
            PSR = -1;
            return;
        }
        if(SP <= 0) {
            System.err.println("Error: Stack Overflow");
            PSR = -1;
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
            System.err.println("Error: Invalid Memory Address");
            PSR = -1;
            return;
        }
        int stackAddressToPopValueFrom = (int) SP;
        int addressToPutValueTo = (int) address;
        memory[addressToPutValueTo] = memory[stackAddressToPopValueFrom];
        SP++;
    }

    private static void dumpMemory(String message, int start, int size) {
        System.out.println(message);
        System.out.println("PC: " + PC + " | Clock: " + Clock);
        System.out.println("PSR: " + PSR);
        System.out.printf("%4s: ", "GPRs");
        for(int i = 0; i < 8; i++) {
            System.out.printf("%5s%d ", "GPR", i+1);
        }
        System.out.println();
        System.out.printf("%4s ", " ");
        for(int i = 0; i < GPRs.length; i++) {
            System.out.printf("%6d ", GPRs[i]);
        }
        System.out.println();
        System.out.printf("%4s ", " ");
        for(int i = 0; i < 10; i++ ){
            System.out.printf("%6s","------");
        }
        System.out.println();
        for (int i = start; i < start + size && i < MEMORY_SIZE; i += 10) {
            System.out.printf("%4d: ", i);
            for (int j = 0; j < 10 && (i + j) < MEMORY_SIZE; j++) {
                System.out.printf("%6d ", memory[i + j]);
            }
            System.out.println();
        }
    }

    private static long[] fetchOperand(long OpMode, long OpGPR) {
        System.out.println("Hello! You are in fetchOperand()");
        long OpAddress;
        long OpValue;
        long[] operand = new long[2];

        if(((int) OpGPR) > 8 || ((int) OpGPR) < 0) {
            System.err.println("Error: Invalid GPR Address");
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
                    System.err.println("Error: Invalid Memory Address");
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
                    System.err.println("Error: Invalid Memory Address");
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
                    System.err.println("Error: Invalid Memory Address");
                    operand[0] = -2;
                    operand[1] = -2;
                    return operand;
                }
                break;
            case 5: // Direct Mode
                System.out.println("You are in fetchOperand Mode 5");
                System.out.println("PC: " + PC);
                OpAddress = memory[(int)PC++];
                System.out.println("PC: " + PC);
                System.out.println("OpMode: " + OpMode );
                System.out.println("HEEEEY it's OpAddress!" + OpAddress);
                if(((int) OpAddress) < 10000 && ((int) OpAddress) >= 0 ) {
                    OpValue = memory[(int) OpAddress];
                    System.out.println("HEEEEY it's OpValue!" + OpValue);
                } else {
                    System.err.println("Error: Invalid Memory Address");
                    operand[0] = -2;
                    operand[1] = -2;
                    return operand;
                }
                System.out.println("You are about to break from switch in FetchOperand");
                break;
            case 6: // Immediate Mode
                System.out.println("You are in FetchOperand Immediate Mode 6");
                System.out.println("PC: " + PC);
                OpValue = memory[(int) PC++];
                System.out.println("OpValue: " + OpValue);
                OpAddress = -1;
                break;
            default: // Invalid Mode
                System.err.println("Error: Invalid Operand Mode");
                operand[0] = -3;
                operand[1] = -3;
                return operand;
        }

        operand[0] = OpAddress;
        operand[1] = OpValue;
        System.out.println("I am returning operand from fetchOperand()");
        return operand;
    }
}
