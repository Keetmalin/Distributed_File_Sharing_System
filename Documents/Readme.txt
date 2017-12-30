---------------------------------------------
Distributed File Sharing System - Using UDP
---------------------------------------------

This program provides a client side implementation for nodes that wish to connect to a distributed network, which is a
file sharing network. This program allows the search and retrieval of files from other ndoes in the network and the
communication from the node to the network, happens via UDP (Unreliable Datagram Protocol).

The given ZIP file contains the following files:

1) Source files (/src)
2) file-sharer.jar
3) pom.xml
4) readme.txt

---------------------------------
Instructions to Run this Program
---------------------------------

Step 1:

Download the given ZIP file, named "Distributed_File_Sharing_System.zip" from Moodle, and extract it to a specific
folder "F". (Any folder is finw)

Step 2:

Open up a terminal (in Linux) or a Command Prompt (in Windows), and navigate to the "F" folder and run the following
command, to build the project.

mvn clean install assembly:single -DskipTests

Step 3:

Next, the make file will be available in the folder named "Target", or the original unzipped file set also contains a
pre-built Make File. you can run which ever you prefer. Navigate to the folder that contains the Make File (JAR), and
run the following command.

(P.S : Before running this command, please ensure that the Bootstrap Server is running).

This command will connect the machine with the Bootstrap server and the distributed file sharing network. You need to
specify the <Bootstrap Server IP> and the <Bootstrap Server Port>, in the following command and run it.

java -jar file-sharer.jar -bs-ip <Bootstrap Server IP> -bs-port <Bootstrap Server Port>

Step 4:

Then, you may enter any of the following commands. Given within brackets are the actions that each command will perform.

(o) stop (will stop the node, gracefully depart from the network and exit the program)
(o) node (this will return the ID of the node, which is being used as reference in the Distributed Network)
(o) state (this will return the state of the current node. The list of available States are given at the bottom of this document)
(o) search <filename> (this will search the network for the <filename> file, and return the list of nodes that contain the file.)
(o) routingTable (this will return the entries in the routing table, which contains the IP address and the Port of the
nodes in the distributed network)
(o) help (this command will provide you with the list of available commands, that can be used with this program)

--------------------------------------------------
Different STATES for a given node in the network
--------------------------------------------------

IDLE - Program hasn't started yet
REGISTERED - Registered in the bootstrap server. That means, we have got 2 nodes (max) to connect to
CONNECTED - Connected to first 2 peers and response arrived along with routing tables, etc
CONFIGURED - Chose a node ID and have undertaken keywords to be looked after by the node as well