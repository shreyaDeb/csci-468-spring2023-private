package edu.montana.csci.csci468.demo;

public class BytecodeDemo {

    int add(int i) {
        return i + 13;
    }

    public static void main(String[] args){
        BytecodeDemo demo = new BytecodeDemo();
        int add = demo.add(12);
        System.out.println(add);
    }

}
