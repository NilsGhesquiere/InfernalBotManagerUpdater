package net.nilsghesquiere;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.nilsghesquiere.gui.swing.InfernalBotManagerUpdaterGUI;
import net.nilsghesquiere.util.ProgramUtil;


public class Main {
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	private static final String PROGRAM_NAME = "InfernalBotManagerClient.exe";
	private static  String MANAGER_MAP = "";
	private static String URL = "";
	private static boolean SOFTSTART = false;
	
	public static void main(String[] args){
		InfernalBotManagerUpdaterGUI gui = new InfernalBotManagerUpdaterGUI();
		LOGGER.info("Starting InfernalBotManager updater");
		try{
			for (int i=0; i<args.length; i++){
				LOGGER.debug("arg[" + i + "] = " + args[i]);
			}
			MANAGER_MAP = args[0];
			URL = args[1];
			//temp workaround for old client sending malformed URL (ending with ")
			if(URL.startsWith("https:\\i")){
				LOGGER.debug("Temp workaround initiated");
				URL = URL.replace('"', '\\');
				URL = URL.replace("https:\\i", "https:\\\\i");
				URL = URL.replace("\\", "/");
			}

			try{
				SOFTSTART = args[2].equals("soft");
			} catch (ArrayIndexOutOfBoundsException e){
				SOFTSTART = false;
			}
			LOGGER.debug("MANAGER_MAP: " + MANAGER_MAP);
			LOGGER.debug("URL: " + URL);
			LOGGER.debug("SOFTSTART: " + SOFTSTART);
			try {
				//wait for process to close (once we have an exe we can force close it)
				LOGGER.info("Sleeping for 10 seconds");
				TimeUnit.SECONDS.sleep(10);
			} catch (InterruptedException e) {
				LOGGER.error("Failure during sleep");
			}
			updateClient();
		} catch (ArrayIndexOutOfBoundsException e ){
			LOGGER.error("Args not found");
		}
		System.exit(0);
	}

	
	public static void updateClient() {
		if(ProgramUtil.downloadFileFromUrl(MANAGER_MAP, URL, PROGRAM_NAME)){
			boolean error = false;

			Path clientPath = Paths.get(MANAGER_MAP + "\\" + PROGRAM_NAME);
			Path clientBackupPath = Paths.get(MANAGER_MAP + "\\backup\\" + PROGRAM_NAME + ".bak");
			Path newClientPath= Paths.get(MANAGER_MAP + "\\downloads\\" + PROGRAM_NAME);
			Path iniPath = Paths.get(MANAGER_MAP + "\\Settings.ini");
			
			//Check if all required files exist
			boolean oldClientExists = Files.exists(clientPath);
			boolean newClientExists = Files.exists(newClientPath);
			boolean iniExists = Files.exists(iniPath);
			
			LOGGER.debug("clientPath:" + clientPath);
			LOGGER.debug("clientBackupPath:" + clientBackupPath);
			LOGGER.debug("newClientPath:" + newClientPath);
			LOGGER.debug("iniPath:" + iniPath);
			
			ProgramUtil.createBackupDir(MANAGER_MAP);
			if (oldClientExists && newClientExists && iniExists){
				//Delete old backup
				boolean oldBackupExists = Files.exists(clientBackupPath);
				if (oldBackupExists){
					try {
						Files.delete(clientBackupPath);
						LOGGER.info("Deleted old backup");
					} catch (IOException e) {
						LOGGER.error("Failure deleting old backup");
						LOGGER.debug(e.getMessage());
						error = true;
					}
				}
				//Move old client to backup as .bak file
				if (!error){
					try {
						Files.copy(clientPath,clientBackupPath, StandardCopyOption.REPLACE_EXISTING);
						LOGGER.info("Backed up old client");
					} catch (IOException e) {
						LOGGER.error("Failure backing up old client");
						LOGGER.debug(e.getMessage());
						error = true;
					}
				}
				//Delete old client
				if (!error){
					try {
						Files.delete(clientPath);
						LOGGER.info("Deleted old client");
					} catch (IOException e) {
						LOGGER.error("Failure deleting old client");
						LOGGER.debug(e.getMessage());
						error = true;			
					}
				}
				//Move new client to managermap
				if (!error){
					try {
						Files.copy(newClientPath,clientPath, StandardCopyOption.REPLACE_EXISTING);
						LOGGER.info("Moved the new client");
					} catch (IOException e) {
						LOGGER.debug("Failure moving the new client");
						error = true;
					}
				}
			
				//Start the client
				if (!error){
					try {
						LOGGER.info("Client update completed, starting client");
						String command = clientPath.toString();
						String arg0 = iniPath.toString();
						String arg1 = "";
						if (SOFTSTART){
							arg1 = "soft";
						} else {
							arg1 = "hard";
						}
						ProcessBuilder pb = new ProcessBuilder(command, arg0, arg1);
						pb.directory(new File(MANAGER_MAP));
						pb.redirectErrorStream(true);
						Process p = pb.start();
						pb.command().stream().forEach(e -> LOGGER.debug(e));
					} catch (IOException e) {
						LOGGER.error("Failure starting client");
						LOGGER.debug(e.getMessage());
					}
				}
			} else {
				if(!oldClientExists){
					LOGGER.error("Old client not found");
				} 
				if(!newClientExists) {
					LOGGER.error("New client not found");
				} 
				if(!iniExists){
					LOGGER.error("Ini not found");
				}	
			}
		} else{
			LOGGER.error("Failed to download the update");
		}
	}
	
}
