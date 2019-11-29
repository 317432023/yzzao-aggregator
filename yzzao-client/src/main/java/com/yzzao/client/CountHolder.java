package com.yzzao.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class CountHolder {
	private static Map<String, Integer> Counter = new HashMap<>();
	
	public synchronized static void plus(Integer machineID) {
		Integer count = Counter.get(machineID.toString());
		if(count == null) {
			Counter.put(machineID.toString(), new Integer(1));
		}else {
			Counter.put(machineID.toString(), count+1);
		}
	}
	
	public synchronized static void printAll() {
		System.out.println(String.format("%s\t%s\t%s","No.","MachineID","Packets Received"));
		System.out.println("============================");
		int i=0;
		for( Entry<String, Integer> entry : Counter.entrySet() ) {
			System.out.println(String.format("%-5d\t%s\t%010d",++i,entry.getKey(),entry.getValue()));
		}
	}
	

	public static void main(String[] args) {
		printAll();
//		System.out.println(String.format("%s\t%s\t%s","序号","机台号","数据包计数"));
//		System.out.println("=======================");
//		System.out.println(String.format("%-5d\t%s\t%010d",111,"100",100));
//		System.out.println(String.format("%-5d\t%s\t%010d",5555,"10000",100000));
	}
}
