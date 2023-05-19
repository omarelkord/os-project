import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Kernel {

    private Mutex fileAccess;

    private Mutex input;
    private Mutex printer;
    private Scheduler scheduler;

    private Pair<String, Object>[] memory;
    private ArrayList<Object> hardDisk;

    public Kernel() {
        this.scheduler = new Scheduler();
        this.fileAccess = new Mutex(this.scheduler);
        this.input = new Mutex(this.scheduler);
        this.printer = new Mutex(this.scheduler);
        this.memory = new Pair[40];
        this.hardDisk = new ArrayList<>();
    }

    public Object readFromDisk(String filename) {

        return null;
    }

    public void writeToDisk(String filename, Object data) {

    }

    public void printData(Object data) {
        System.out.println(data);
    }

    public Object takeInput() {
        System.out.println("Please enter a value: ");
        Scanner sc = new Scanner(System.in);
        return sc.next();  //check this de habda ;)
    }

    public Object readFromMemory(int pc) {
        return memory[pc];
    }

    public void writeToMemory(Object data) {
        //mesh aarfen :)))))
    }

    public void executeInstruction(Process process, String instruction) throws FileNotFoundException {
        Scanner scanner = new Scanner(System.in);

        String[] ins = instruction.split(" ");

        if (ins[0].equals("assign")) {
            Object toAssign;
            String varName = ins[1];

            if (ins[2].equals("input")) {
                System.out.println("Please enter a value: ");
                toAssign = scanner.nextLine();
//                System.out.println("x = " + input);

            } else toAssign = ins[2];

            try {
                toAssign = Integer.parseInt((String) toAssign);
            } catch (Exception e) {
            }

            // write (varName: toAssign) in process address space
            process.addVar(varName, toAssign);

        } else if (ins[0].equals("print")) {
            // fetch x from process address space
            Object var = process.getVar(ins[1]);
            System.out.println(var);
        } else if (ins[0].equals("writeFile")) {
            String fileName = (String) process.getVar(ins[1]);
            Object data = process.getVar(ins[2]);

            PrintWriter pw = new PrintWriter(new FileOutputStream(fileName, true));
            pw.append(data.toString());
            pw.append("\n");
            pw.close();

        } else if (ins[0].equals("printFromTo")) {
            int x = (int) process.getVar(ins[1]);
            int y = (int) process.getVar(ins[2]);

            for (int i = x; i <= y; i++) {
                System.out.println(i);
            }
        } else if (ins[0].equals("semWait")) {

            switch (ins[1]) {
                case "userOutput" -> printer.semWait(process);
                case "userInput" -> input.semWait(process);
                case "file" -> fileAccess.semWait(process);
            }

        } else if (ins[0].equals("semSignal")) {
            switch (ins[1]) {
                case "userOutput" -> printer.semSignal(process);
                case "userInput" -> input.semSignal(process);
                case "file" -> fileAccess.semSignal(process);
            }
        }
    }

    public void run(Process process) throws FileNotFoundException {
        int pc = process.getPC();
        process.setPC(pc + 1);

        String ins = (String) memory[pc].getValue();
        executeInstruction(process, ins);
    }

    public static String getSpaces(int n) {
        String spaces = "";
        for (int i = 0; i < n; i++) {
            spaces += " ";
        }

        return spaces;
    }

    public static void displayMemory(Pair<String, Object>[] memory) {

        System.out.println(getSpaces(5) + "+------------------------------+");

        int length = 30;

        for (int i = 0; i < memory.length; i++) {

            Pair<String, Object> p = memory[i];

            String strMemAddr = i + "";
            String addrWhitespc = getSpaces(4 - strMemAddr.length());

            System.out.print(i + ":" + addrWhitespc);

            int pairSize;
            if (p == null) {
                pairSize = 0;
            } else if (p.getKey() == null && p.getValue() == null) {
                pairSize = 8;       // reserved
            } else {
                pairSize = ((String) p.getKey()).length() + p.getValue().toString().length() + 4;
            }
            int numSpaces = (length - pairSize) / 2;
            String spaces = getSpaces(numSpaces);

            System.out.print("|" + spaces);
            System.out.print(p == null ? "" : (p.getKey() == null && p.getValue() == null) ? "reserved" : p);
            System.out.println(spaces + (pairSize % 2 == 0 ? "" : " ") + "|");

            if (i == memory.length - 1)
                System.out.println(getSpaces(5) + "+------------------------------+");
            else
                System.out.println(getSpaces(5) + "--------------------------------");
        }
    }

    public Pair<Integer, Integer> fitsInMemory(int progSize) throws IOException {


        int empty = 0;
        int j = 0;
        int startIdx = 0;

        for (int i = 0; i < memory.length; i++) {
            Pair p = memory[i];

            if (memory[i] == null && memory[j] == null) {
                empty++;
            } else if (memory[i] == null) {           // MEMORY[j] != NULL
                empty = 1;
                startIdx = i;
            } else {
                empty = 0;
                startIdx = -1;
            }

            if (empty >= progSize) {
//                System.out.println("Suitable slot found from " + startIdx + " to " + (startIdx + progSize - 1));
                return new Pair<Integer, Integer>(startIdx, startIdx + progSize - 1);
            }

            j = i;
        }

        return null;
    }

    // (x)
    // null
    // null
    // null
    // y

    public Process createNewProcess(String fileName) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String line;
        int progSize = 0;

        //Accumulate to count instructions
        while ((line = br.readLine()) != null) {
            progSize++;
        }
        br.close();

        //Plus 3 to accomodate for 3 locations for future variables
        Pair<Integer, Integer> range = fitsInMemory(progSize + 3);

        if (range == null) {
            // RAM CANNOT ACCOMMODATE PROCESS
            // SWAP FROM DISK
        } else {
            PCB pcb = new PCB(range.getKey(), range.getKey(), range.getValue());
            Process process = new Process(pcb, memory);

            br = new BufferedReader(new FileReader(fileName));

            for (int i = pcb.getStartBoundary(); (line = br.readLine()) != null; i++) {
                Pair<String, Object> pair = new Pair<>("ins", line);
                memory[i] = pair;
            }


            //Reserve places for future variable insertions
            for (int j = pcb.getEndBoundary(); j > pcb.getEndBoundary() - 3; j--) {
                memory[j] = new Pair<>();
            }

            br.close();

            return process;
        }

        return null;
    }

    public static void main(String[] args) throws IOException {
        Pair<String, Object>[] memory = new Pair[40];

        Kernel kernel = new Kernel();
        kernel.memory = memory;

        Process p1 = kernel.createNewProcess("Program_1.txt");
//        displayMemory(memory);
//        System.out.println(p1.getStartBoundary() + ", " + p1.getEndBoundary());

        Process p2 = kernel.createNewProcess("Program_2.txt");
//        displayMemory(memory);

        Process p3 = kernel.createNewProcess("Program_3.txt");
//        displayMemory(memory);

        //semWait userInput
        kernel.run(p2);

        //assign a input
        kernel.run(p2);
//        displayMemory(memory);

        //assign b input
        kernel.run(p2);
//        displayMemory(memory);

        //semSignal userInput
        kernel.run(p2);

        //semWait file
        kernel.run(p2);

        //writeFile a b
        kernel.run(p2);

        //semSignal file
        kernel.run(p2);

//        System.out.println(p2.getPC());


//        memory[1] = new Pair<>("x", new Integer(3));
//        memory[2] = new Pair<>("ins", "assign x input");
//        memory[3] = new Pair<>("ins", "print y");
//
//
//        displayMemory(memory);
//        System.out.println();


//        System.out.println(String.format("%16s", "null"));

//        loadProcessIntoMemory();
//        Process p = kernel.createNewProcess("program_1.txt");
//        System.out.println(p.getStartBoundary() + ", " + p.getEndBoundary());
//        displayMemory(memory);
//        kernel.run(p);
//        kernel.run(p);
//        kernel.run(p);
//        kernel.run(p);

//        kernel.fitsInMemory(memory, 4);

//        try {
//            kernel.createNewProcess("program_1.txt");
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }

    }


}
