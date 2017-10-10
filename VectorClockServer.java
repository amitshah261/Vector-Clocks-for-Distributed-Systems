package project2_1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class VectorClockServer implements Runnable{
	
	int port;
	static Process process;
	static ServerProperties serverProperties = null;
	Scanner scan = new Scanner(System.in);
	int noOfOperations = 3, maxAmount = 100;
	int serverId;
	
	VectorClockServer(String hostname){
		
		port = 8400;
		serverProperties = ServerProperties.getServerPropertiesObject();
		hostname += serverProperties.suffix;
		System.out.println(hostname);
		process = new Process(hostname);
		serverId = serverProperties.processId.get(hostname); 
	}
	
	public void run(){
		System.out.println("Entered Run");
		while(true){
			String threadName = Thread.currentThread().getName();
			System.out.println(threadName);
			if(threadName.equals("local")){
				
				//Pick random values
				Random rand = new Random();
				int randomChoice = rand.nextInt(noOfOperations);
				int randomAmount = rand.nextInt(maxAmount + 1);
				int currentAmount = process.balance;
				
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				synchronized(process.vector){
					switch(randomChoice){
						case 0: { // Deposit
//							deposit(currentAmount, randomAmount);
							process.vector[serverId] += 1;
							process.balance += randomAmount;
							System.out.println("Deposit of Amount: " + currentAmount +" Success, New Balance: " + process.balance);
							System.out.println("New Vector After Deposit: ");
							System.out.println(Arrays.toString(process.vector));
							break;
						}
						case 1: { // Withdraw
//							withdraw(currentAmount, randomAmount);
							if(currentAmount - randomAmount > 0){
								process.vector[serverId] += 1;
								process.balance -= randomAmount;
								System.out.println("Withdrawal of Amount: " + randomAmount +" Success, New Balance: " + process.balance);
								System.out.println("New Vector After Withdraw: ");
								System.out.println(Arrays.toString(process.vector));
							}
							else{
								System.err.println("Not enough Balance to withdraw " + randomAmount + " Current Balance: " + currentAmount);
							}
							break;
						}
						case 2: { // Transfer
							transfer(currentAmount, randomAmount, rand);
							break;
						}
						default: {
							System.out.println("Invalid Choice");
							break;
						}
					}
				}
				
			}
			else if (threadName.equals("transferController")){
				
				ServerSocket serverSocket = null;
				Socket clientSocket = null;
				
				try{
					serverSocket = new ServerSocket(port);
					
					System.out.println("Listening on port: " + port);
					
					clientSocket = serverSocket.accept();
					DataInputStream DI = new DataInputStream(clientSocket.getInputStream());
					ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
					
					int currentAmount = DI.readInt();
					
					process.balance += currentAmount; 
					System.out.println("Transfer of Amount: " + currentAmount +" Success, New Balance: " + process.balance);
					
					int[] incomingVector = (int[]) in.readObject();
					
					for(int index = 0; index < incomingVector.length; index++){
						if (incomingVector[index] > process.vector[index]){
							process.vector[index] = incomingVector[index] + 1; 
						}
						else{
							process.vector[index] += 1;
						}
					}
					System.out.println("New Vector After Transfer: ");
					System.out.println(Arrays.toString(process.vector));
				}
				catch(Exception e){
					errorMessage(e);
				}
				finally{
					try {
						serverSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			else{
				break;
			}
		}
		
	}
	
	void deposit(int currentAmount, int randomAmount){
		process.vector[serverId] += 1;
		process.balance += randomAmount;
		System.out.println("Deposit of Amount: " + currentAmount +" Success, New Balance: " + process.balance);
		System.out.println("New Vector After Deposit: ");
		System.out.println(Arrays.toString(process.vector));
	}
	
	void withdraw(int currentAmount, int randomAmount){
		if(currentAmount - randomAmount > 0){
			process.vector[serverId] += 1;
			process.balance -= randomAmount;
			System.out.println("Withdrawal of Amount: " + randomAmount +" Success, New Balance: " + process.balance);
			System.out.println("New Vector After Withdraw: ");
			System.out.println(Arrays.toString(process.vector));
		}
		else{
			System.err.println("Not enough Balance to withdraw " + randomAmount + " Current Balance: " + currentAmount);
		}
	}
	
	void transfer(int currentAmount, int randomAmount, Random rand){
		Socket clientSocket = null; 
		int randomProcess = rand.nextInt(serverProperties.numberOfProcesses);
		
		while(randomProcess == serverId){
			randomProcess = rand.nextInt(serverProperties.numberOfProcesses);
		}
		
		String hostname = serverProperties.servers[randomProcess];
		
		if((currentAmount - randomAmount) > 0){
			
			process.vector[serverId] += 1;
			process.balance -= randomAmount;
			try{
				clientSocket = new Socket(hostname, port);
				ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
				DataOutputStream DO = new DataOutputStream(clientSocket.getOutputStream());
				DO.writeInt(randomAmount);
				out.writeObject(process.vector);
			}
			catch (Exception e){
				e.printStackTrace();
			}
			finally{
				try {
					clientSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		else{
			System.err.println("Not enough Balance!");
		}
	}
	
	void errorMessage(Exception e){
		e.printStackTrace();
	}
	
	public static void main(String[] args) throws UnknownHostException {
		
		VectorClockServer vc = new VectorClockServer( InetAddress.getLocalHost().getHostName() + "");
		Thread t1 = new Thread(vc);
		t1.setName("local");
		Thread t2 = new Thread(vc);
		t2.setName("transferController");
		t1.start();
		t2.start();
		
		
	}
	
}
