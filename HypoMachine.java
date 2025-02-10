// Sasha Basova, Student ID: 301089119
// Homework 1 2/9/2025
// Hardware Simulator HYPO Machine
// Implemented with Java
// Performs: initialize, load an executable file into memory, execute program, output memory dump into console and basova-hw1_output.txt file
// Error Codes: -1 if there is an error; 0 if there are no errors. 
// Return values: Only methods that return are absoluteLoader() which returns Long and fetchOperand() which return Long[]
// Global variables for the class HypoMachine: memory, GPRs, MAR, MBR, IR, SP, PC, PSR, Clock
// Constants: MEMORY_SIZE

import java.io.*;
import java.util.*;


public class HypoMachine {
    // Memory and Registers
    private static final int MEMORY_SIZE = 10000;
    private static long[] memory = new long[MEMORY_SIZE]; // memory array of the length MEMORY_SIZE
    private static long[] GPRs = new long[8]; // General-purpose registers
    private static long MAR, MBR, IR, SP, PC, PSR, Clock;  // Special registers

    public static void main(String[] args) {
        try (
            PrintWriter fileWriter = new PrintWriter(new FileWriter("basova-hw1_output.txt")); // FileWriter to output into basova-hw1_output.txt
            PrintWriter consoleWriter = new PrintWriter(System.out, true) // Auto-flushing
        ) {

        initializeSystem();
        Scanner scanner = new Scanner(System.in);  // Scanner to read from input file
        System.out.print("Enter executable file name: ");
        String fileName = scanner.nextLine();
        long startAddress = absoluteLoader(fileName); // Return value from absoluteLoader() is stored in startAddress, this is where the execution begins if StartAddress >= 0
        if (startAddress >= 0) {
            PC = startAddress; // PC is set to startAddress
            dumpMemory(fileWriter, consoleWriter, "After Loading Program", 0, 100); // Dump memory before the execution
            executeProgram();  
        } 
       
        dumpMemory(fileWriter, consoleWriter, "After Executing Program", 0, 100); // Dump memory after execution
        scanner.close();   
        System.out.println("Memory dump written to basova-hw1_output.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    // Function: initializeSystem
    // Tasks
    //     Initializes array memory and array GPRs with 0s, initializes MAR, MBR, IR, PC, PSR, Clock to 0; initializes SP to 9999
    // Input Parameters: None
    // Output Parameters: None
    // Return: void
    private static void initializeSystem() {
        Arrays.fill(memory, 0);
        Arrays.fill(GPRs, 0);
        MAR = MBR = IR = PC = PSR = Clock = 0;
        SP = 9999;
    }

    // Function: absoluteLoader
    // Tasks
    //     Reads from input file and loads the content into memory
    // Input Parameters: fileName        Name of the executable file
    // Return: Long     -1 if there is an error or start address if no error
    // Error Invalid Instruction address, Error No end mark is found, Loading Error
    private static long absoluteLoader(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split("\\s+"); // stores two parts of the string line into an array parts
                if (parts.length == 2) {
                    long address = Long.parseLong(parts[0]); // stores first element of array parts into Long address
                    long value = Long.parseLong(parts[1]); // stores second element of parts into Long value
                    if (address < 0) return value; // End of program, return PC start
                    if (address >= MEMORY_SIZE) {
                        System.err.println("Error: Invalid Instruction Address");
                        return -1; // Invalid address
                    } 
                    memory[(int) address] = value; // stores value at the memory index which equals (int) address
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading file: " + e.getMessage());
            return -1;
        }
        System.err.println("Error: No End Marker Found");
        return -1; // Error if no end marker
    }


    // Function: executeProgram
    // Tasks
    //     While isToExecute is true, executes the instructions stored in memory. Determines opcode, Op1Mode, Op2Mode, Op1GPR, Op2GPR and runs execution according to opcode
    // Input Parameters: none
    // Return: void
    // Error Invalid PC address, Error No end mark is found, Loading Error
    private static void executeProgram() {
        boolean isToExecute = true;
        while (isToExecute) {
            if (PC < 0 || PC >= MEMORY_SIZE) {
                System.err.println("Error: Invalid PC Address");
                break;
            }
            // System.out.println("You are in the ExecuteProgram(). Next line of instruction.");
            MAR = PC; // Stores current PC value into MAR
            MBR = memory[(int) MAR]; // Stores the value from the memory[(int) MAR] into MBR
            IR = MBR; // Stores MBR value into IR
            PC++; // increments PC
            long opcode = (IR / 10000);
            long Op1Mode = ((IR % 10000) / 1000);
            long Op1GPR = (((IR % 10000) % 1000) / 100);
            long Op2Mode = ((((IR % 10000) % 1000) % 100) / 10);
            long Op2GPR = ((((IR % 10000) % 1000) % 100) % 10);

            switch ((int)opcode) {
                case 0: // Halt
                    isToExecute = false; // Stops while loop -> stops the execution 
                    Clock += 12;
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
                    isToExecute = false; // Stops the execution
            }
            
        }
    }


    // Function: executeArithmetic
    // Tasks
    //     Executes Add, Subtract, Multiply, Divide according to the operator parameter
    // Input Parameters: char operator, long Op1Mode, long Op1GPR, long Op2Mode, long Op2GPR
    // Return: void
    // Error: Division by Zero, Error: Invalid Operator, Error: Destination operand cannot be immediate value
    private static void executeArithmetic( char operator, long Op1Mode, long Op1GPR, long Op2Mode, long Op2GPR ) {
       
        long[] operand1 = new long[2];
        long[] operand2 = new long[2];

        operand1 = fetchOperand(Op1Mode, Op1GPR);
        long Op1Value = operand1[1];
        long Op1Address = operand1[0];

        operand2 = fetchOperand(Op2Mode, Op2GPR);
        long Op2Value = operand2[1];
        
        long result;

        // System.out.println("You are in executeArithmetic() after fetching operands and checking that they are > 0");

        if(Op2Value == 0 && operator == '/') {
            System.err.println("Error: Division by Zero");
            return;
        }


        switch (operator) {
            case '+': result = Op1Value + Op2Value; Clock += 3; break;
            case '-': result = Op1Value - Op2Value; Clock += 3; break;
            case '*': result = Op1Value * Op2Value; Clock += 6; break;
            case '/': result = (Op2Value != 0) ? Op1Value / Op2Value : 0; Clock += 6; break;
            default: 
                System.err.println("Error: Invalid Operator");
                return;
        }

        // System.out.println("Result: " + result);

        if(Op1Mode == 1) {
            GPRs[(int)Op1GPR - 1] = result; // If Op1Mode == 1, stores the result value into GPR
        } else if (Op1Mode == 6) {
            System.err.println("Destination operand cannot be immediate value");
            return;
        } else {
            if(Op1Address < 0 ) {
                System.err.println("Error: Invalid Operand Address");
                return;
            }
            memory[(int) Op1Address] = result; // All other values of Op1Mode (except 1 and 6) -> stores result into memory
        }

    }

    // Function: executeMove
    // Tasks
    //     Executes Move 
    // Input Parameters: long Op1Mode, long Op1GPR, long Op2Mode, long Op2GPR
    // Return: void
    // Error: Destination operand cannot be immediate value
    private static void executeMove(long Op1Mode, long Op1GPR, long Op2Mode, long Op2GPR) {
        long Op1Address = fetchOperand(Op1Mode, Op1GPR)[0];
        long Op2Value = fetchOperand(Op2Mode, Op2GPR)[1];
        

        if(Op1Mode == 1) {
            GPRs[(int) Op1GPR - 1] = Op2Value;
        } else if(Op1Mode == 6){
            System.err.println("Error: Destination operand cannot be immediate value");
            return;
        } else {
            if(Op1Address < 0 ) {
                System.err.println("Error: Invalid Operand Address");
                return;
            }
            memory[(int)Op1Address] = Op2Value;
        } 
        Clock += 2;
    }
    
    // Function: executeBranch
    // Tasks
    //     Executes Branch (Jump to the specified address in memory) 
    // Input Parameters: none
    // Return: void
    // Error: Invalid Branch Address
    private static void executeBranch() {
        long branchAddr = memory[(int) PC++];
        if (branchAddr >= 0 && branchAddr < MEMORY_SIZE) {
            PC = branchAddr;
        } else {
            System.err.println("Error: Invalid Branch Address");
        }
        Clock += 2;
    }


    // Function: executeBranchOnCondition
    // Tasks
    //     Executes Branch on Condition (Jump to the specified address in memory if the condition is met) 
    // Input Parameters: long Op1Mode, long Op1GPR, java.util.function.LongPredicate condition
    // Return: void
    // Error: Invalid Memory Address
    private static void executeBranchOnCondition(long Op1Mode, long Op1GPR, java.util.function.LongPredicate condition) {

        long Op1Value = fetchOperand(Op1Mode, Op1GPR)[1];
        
        // System.out.println("Inside executeBranchOnCondition after fetching Op1Value");
        
        long branchAddr = memory[(int) PC];
        if (branchAddr < 0 || branchAddr >= MEMORY_SIZE) {
            System.err.println("Error: Invalid Memory Address");
            return;
        }
       
        if (condition.test(Op1Value)) {
            PC = branchAddr;
        } else {
            PC++;
        }
        Clock += 4;
        
    }


    // Function: executePush
    // Tasks
    //     Adds element on top of the stack 
    // Input Parameters: none
    // Return: void
    // Error: Invalid Memory Address, Error: Stack Overflow
    private static void executePush() {
        long address = memory[(int) PC];
        PC++;
        if(address >= MEMORY_SIZE || address < 0) {
            System.err.println("Error: Invalid Memory Address");
            return;
        }
        if(SP <= 0) {
            System.err.println("Error: Stack Overflow");
            return;
        }
        int addressToGetValueFrom = (int) address;
        int stackAddressToPushValueTo = (int) SP;
        memory[stackAddressToPushValueTo] = memory[addressToGetValueFrom];
        SP--; // stack grows downwards
        Clock += 2;
    }

    // Function: executePop
    // Tasks
    //     Removes the top element from the stack
    // Input Parameters: none
    // Return: void
    // Error: Invalid Memory Address
    private static void executePop() {
        long address = memory[(int) PC++];
        if(address >= MEMORY_SIZE || address < 0) {
            System.err.println("Error: Invalid Memory Address");
            return;
        }
        int stackAddressToPopValueFrom = (int) SP;
        int addressToPutValueTo = (int) address;
        memory[addressToPutValueTo] = memory[stackAddressToPopValueFrom];
        SP++;
        Clock += 2;
    }



    // Function: dumpMemory
    // Tasks
    //     Dumps memory of the specified size into console and output file
    // Input Parameters: PrintWriter fileOut, PrintWriter consoleOut, String message, int start, int size
    // Return: void
    private static void dumpMemory(PrintWriter fileOut, PrintWriter consoleOut, String message, int start, int size) {
        // Print to both file and console
        printBoth(fileOut, consoleOut, message);
        printBoth(fileOut, consoleOut, "PC: " + PC + " | Clock: " + Clock + " | SP: " + SP);
        printBoth(fileOut, consoleOut, "PSR: " + PSR);
        
        fileOut.printf("%4s: ", "GPRs");
        consoleOut.printf("%4s: ", "GPRs");
        for (int i = 0; i < 8; i++) {
            fileOut.printf("%5s%d ", "GPR", i + 1);
            consoleOut.printf("%5s%d ", "GPR", i + 1);
        }
        fileOut.println();
        consoleOut.println();

        fileOut.printf("%4s ", " ");
        consoleOut.printf("%4s ", " ");
        for (int i = 0; i < GPRs.length; i++) {
            fileOut.printf("%6d ", GPRs[i]);
            consoleOut.printf("%6d ", GPRs[i]);
        }
        fileOut.println();
        consoleOut.println();

        fileOut.printf("%4s ", " ");
        consoleOut.printf("%4s ", " ");
        for (int i = 0; i < 10; i++) {
            fileOut.printf("%6s", "------");
            consoleOut.printf("%6s", "------");
        }
        fileOut.println();
        consoleOut.println();

        for (int i = start; i < start + size && i < MEMORY_SIZE; i += 10) {
            fileOut.printf("%4d: ", i);
            consoleOut.printf("%4d: ", i);
            for (int j = 0; j < 10 && (i + j) < MEMORY_SIZE; j++) {
                fileOut.printf("%6d ", memory[i + j]);
                consoleOut.printf("%6d ", memory[i + j]);
            }
            fileOut.println();
            consoleOut.println();
        }
    }

    private static void printBoth(PrintWriter fileOut, PrintWriter consoleOut, String message) {
        fileOut.println(message);
        consoleOut.println(message);
    }



     // Function: fetchOperand
    // Tasks
    //     fetches operand according to OpMode and OpGPR
    // Input Parameters: long OpMode, long OpGPR
    // Return: long[]
    private static long[] fetchOperand(long OpMode, long OpGPR) {
        // System.out.println("You are in fetchOperand()");
        long OpAddress;
        long OpValue;
        long[] operand = new long[2]; // creates an array to store operand's value and address

        if(((int) OpGPR) > 8 || ((int) OpGPR) < 0) {
            System.err.println("Error: Invalid GPR Address");
            operand[0] = -2;
            operand[1] = -2;
            return operand;
        }

        switch((int)OpMode) {
            case 1: // Register Mode
                OpValue = GPRs[(int)OpGPR - 1];
                OpAddress = -1;
                break;
            case 2: // Register Deferred Mode
                OpAddress = GPRs[(int) OpGPR - 1];
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
                OpAddress = GPRs[(int) OpGPR - 1];
                if(((int) OpAddress) < 10000 && ((int) OpAddress) >= 0 ) {
                    OpValue = memory[(int) OpAddress];
                } else {
                    System.err.println("Error: Invalid Memory Address");
                    operand[0] = -2;
                    operand[1] = -2;
                    return operand;
                }
                GPRs[(int) OpGPR - 1]++;
                break;
            case 4: // Autodecrement mode
                OpAddress = --GPRs[(int) OpGPR - 1];
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
                // System.out.println("You are in fetchOperand Mode 5");
                OpAddress = memory[(int)PC++];
                if(((int) OpAddress) < 10000 && ((int) OpAddress) >= 0 ) {
                    OpValue = memory[(int) OpAddress];
                } else {
                    System.err.println("Error: Invalid Memory Address");
                    operand[0] = -2;
                    operand[1] = -2;
                    return operand;
                }
                break;
            case 6: // Immediate Mode
                // System.out.println("You are in FetchOperand Immediate Mode 6");
                OpValue = memory[(int) PC++];
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
        return operand;
    }
}
