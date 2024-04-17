import java.text.DecimalFormat;
import java.util.BitSet;
import java.util.Random;
import java.util.*;
import java.io.*;

class Constants {
    public static final int NUM_ITERATIONS = 250000;
    public static final int NUM_OF_BITS = 1000000;
}

public class Main {

    public static void main(String[] args){
        int rows = 4;
        int cols = 3;
        double[] p_values = new double[]{0.00001, 0.00005, 0.0001, 0.0005, 0.001, 0.005, 0.01, 0.05, 0.1, 0.2, 0.5};
        final DecimalFormat df = new DecimalFormat("0.00000000");
        long startTime = System.currentTimeMillis();

        List<String[]> rowsList = new ArrayList<>();

        BitMessage bm = new BitMessage(rows, cols);
        bm.defineGMatrix();
        bm.defineHMatrix();

        rowsList.add(new String[]{"p", "E", "D"});

        for (double pValue : p_values) {
            double numOfReceivedErrors = 0.0;
            double numOfDecodedErrors = 0.0;
            for (int j = 0; j < Constants.NUM_ITERATIONS; j++) {
                bm.clearAll();
                bm.generateOriginalMessage(0.5, rows);
                bm.multiplyByMatrix(rows, rows + cols, bm.getOriginalMessage(), bm.getEncodedMessage(), bm.getG());
                bm.generateReceivedMessage(pValue, rows + cols);
                numOfReceivedErrors += bm.receivedErrorCardinality();
                bm.multiplyByMatrix(rows + cols, cols, bm.getReceivedMessage(), bm.getErrorBitSet(), bm.getH());
                bm.decodeMessage();
                numOfDecodedErrors += bm.decodedErrorCardinality();
            }
            rowsList.add(new String[]{String.valueOf(pValue),
                    df.format(numOfReceivedErrors / Constants.NUM_OF_BITS),
                    df.format(numOfDecodedErrors / Constants.NUM_OF_BITS)});
        }

        try (FileWriter csvWriter = new FileWriter("output.csv")) {
            for (String[] row : rowsList) {
                csvWriter.append(String.join(",", row));
                csvWriter.append("\n");
            }
            System.out.println("Data has been written to output.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        System.out.println(STR."Execution time: \{executionTime} milliseconds");
    }
}

class BitMessage {
    private final BitSet[] G, H;
    private final BitSet original, encoded, errorPosition;
    private final int rows, cols;
    private BitSet received, decoded;
    public BitMessage(int rows, int cols){
        this.original = new BitSet(rows);
        this.decoded = new BitSet(rows);
        this.errorPosition = new BitSet(cols);
        this.received = new BitSet(rows+cols);
        this.encoded = new BitSet(rows+cols);
        this.rows = rows;
        this.cols = cols;

        this.G = new BitSet[rows];
        for(int i = 0; i < rows; i++) G[i] = new BitSet(rows+cols);

        this.H = new BitSet[rows+cols];
        for(int j = 0; j < (rows+cols); j++) H[j] = new BitSet(cols);
    }
    public BitSet getOriginalMessage(){
        return original;
    }
    public BitSet getReceivedMessage(){
        return received;
    }
    public BitSet getEncodedMessage(){
        return encoded;
    }
    public BitSet getDecodedMessage(){
        return decoded;
    }
    public BitSet getErrorBitSet(){
        return errorPosition;
    }
    public BitSet[] getG(){
        return G;
    }
    public BitSet[] getH(){
        return H;
    }
    public int getErrorPosition(){
        BitSet temp = new BitSet();
        for(int i = 0; i < rows; i++){
            for (int j = 0; j < cols; j++){
                if(H[i].get(j))
                    temp.set(j);
            }
            if(errorPosition.equals(temp)){
                return i;
            }
            temp.clear();
        }
        return -1;
    }
    public void generateOriginalMessage(double q, int size){
        Random r = new Random();
        for(int i = 0; i < size; i++){
            double probability = r.nextDouble();
            if(probability <= q) original.set(i);
        }
    }
    public void generateReceivedMessage(double p, int size){
        Random r = new Random();
        received = (BitSet) encoded.clone();
        for(int i = 0; i < size; i++) {
            double probability = r.nextDouble();
            if (probability <= p) received.flip(i);
        }
    }
    public void printMessage(BitSet b, int size){
        for (int i = 0; i < size; i++) {
            System.out.print(b.get(i) ? 1 : 0);
        }
        System.out.println();
    }
    public void setInMatrix(int row, int column, BitSet[] m){
        m[row].set(column);
    }
    public void multiplyByMatrix(int numberOfRows, int numberOfCols, BitSet set, BitSet result, BitSet[] m){
        BitSet temp = new BitSet(numberOfRows);
        for(int i = 0; i < numberOfCols; i++){
            for(int j = 0; j < numberOfRows; j++){
                if(m[j].get(i)) {
                    temp.set(j);
                }
            }
            temp.and(set);
            if(temp.cardinality() % 2 == 1) {
                result.set(i);
            }
            temp.clear();
        }
    }
    public void decodeMessage(){
        int i = getErrorPosition();
        decoded = (BitSet) received.clone();
        if(i != -1) decoded.flip(i);
        decoded = decoded.get(0,4);
    }
    public int receivedErrorCardinality(){
        BitSet temp = (BitSet) received.clone();
        temp = temp.get(0,4);
        temp.xor(original);
        return temp.cardinality();
    }
    public int decodedErrorCardinality() {
        decoded.xor(original);
        return decoded.cardinality();
    }
    public void clearAll(){
        original.clear();
        encoded.clear();
        errorPosition.clear();
        received.clear();
        decoded.clear();
    }
    public void defineGMatrix(){
        setInMatrix(0, 0, G);
        setInMatrix(1, 1, G);
        setInMatrix(2, 2, G);
        setInMatrix(3, 3, G);
        setInMatrix(0, 4, G);
        setInMatrix(1, 4, G);
        setInMatrix(2, 4, G);
        setInMatrix(0, 5, G);
        setInMatrix(2, 5, G);
        setInMatrix(3, 5, G);
        setInMatrix(0, 6, G);
        setInMatrix(1, 6, G);
        setInMatrix(3, 6, G);
    }
    public void defineHMatrix(){
        setInMatrix(0, 0, H);
        setInMatrix(1, 0, H);
        setInMatrix(2, 0, H);
        setInMatrix(4, 0, H);
        setInMatrix(0, 1, H);
        setInMatrix(2, 1, H);
        setInMatrix(3, 1, H);
        setInMatrix(5, 1, H);
        setInMatrix(0, 2, H);
        setInMatrix(1, 2, H);
        setInMatrix(3, 2, H);
        setInMatrix(6, 2, H);
    }
}