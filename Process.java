package project2_1;

public class Process {
	
	int id;
	int balance = 1000;
	int[] vector;
	static ServerProperties serverProperties;
	
	Process(String hostname){
		serverProperties = ServerProperties.getServerPropertiesObject();
		vector = new int[ServerProperties.numberOfProcesses];
		this.id = serverProperties.processId.get(hostname);
	}
}
