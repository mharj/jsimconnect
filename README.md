## jSimConnect - a simconnect java client library

Note: this is still work in progress. Library works with named pipes and can be connected 
to MSFS2020. New protocol features are missing.

jSimConnect is a java implementation of a simconnect client library.
SimConnect is a client/server architecture that enables add-ons 
developpers to communicate with a running instance of Microsoft Flight Simulator X.


jSimConnect is distributed under the terms of the LGPL license.

-------------------------------------------------------------------------

## Installation: 


Simply drop jsimconnect.jar into your project. Don't forget to add it to
your classpath.

SimConnect use network sockets (TCP/IP) to communicate with the server, you
should allow it in your security profiles (if you use one)


-------------------------------------------------------------------------

## Documentation: 

Please refer to the javadoc included in this package in the doc/ subdirectory.
The main. Most functions prototypes and communications concepts are similar
to the Microsoft implementation, so the SDK documentation is also a very
good help.

Some samples are included in the samples/ directory.

A technical documentation about the simconnect protocol (independant of
jSimConnect) is providen in the package-doc page for the flightsim.jsimconnect
package. Browse the javadoc to this package index to access it.


-------------------------------------------------------------------------

## FAQ & Tips

### Connection parameters & port numbers 
	
SimConnect uses a TCP connection to communicate with the running
instance of Flight Simulator. The client side application have
to guess connection parameters (address, port and IP version).
The socket parameters can be changed in simconnect.xml on the server 
side, the default behaviour is to choose a random port, listen
only loopback (127.0.0.1) interface and write the value of the
choosen port to the registry database.
Beginning with 0.6, jsimconnect provides small helper methods
to guess the correct port to use by simconnect (by reading
the registry values); see Configuration#findSimConnectPort

FSX SP2 introduced new transport protocol (using proprietary 
windows pipes), but are unsupported by jsimconnect.
	

### Enumerations
	
Most jsimconnect methods using integers as request identifiers are
overriden to accept Enum class (thus user-defined enumerations).
However responses structures are only providing integers, use the
ordinal() method to compare them to your enumerations, i.e. :

```java
	if (res.getRequestId() == MyEnum.MyID.ordinal()) {
		// code
	}
```
	
Integer <-> Enum constant transtyping is not natural in java, so it's
better to provide your own methods. Since you can define new methods
in enumeration, one could use :

```java	
enum MyEnum {
    MyID1,
    MyID2;

    public boolean isEvent(RecvEvent re) {
        return ordinal() == re.getEventID();
    }
}
```

### FSX Service packs & protocol updates
NB: does not deal with IP (4 or 6) protocol !

When using a simconnect instance, some constructors accepts a protocol
version number. These version indicates the simconnect protocol to
emulate when talking to the remote FSX. The different FSX updates
included new features to the simconnect protocol & client library, but 
also broke backward compatibility when the simconnect client is newer 
than the remote Flight Simulator instance.
In the MS Implementation, this is resolved by statically linking (with
or without a manifest) the  client application to a specific version of 
the library installed in the Side-by-Side repository.

JSimconnect uses the version given to the constructor to _emulate_ an 
older simconnect protocol client, thus allowing to connect to older FSX 
versions. Note that by default, the protocol number used is the most up
to date, and consequently a VERSION_MISMATCH exception will be received
upon connecting to older FSX version.

As of jsimconnect 0.7 the supported protocol numbers are :
- 0x2 (RTM version). Note that 0x1 (possibly from fsx beta) is not supported 
- 0x3 (SP1). 
- 0x4 (SP2/Acceleration) 

When specifying a protocol version to emulate, some functions will be
inhibited as they may not be available in the current version. For
instance modeless UI, are not available before SP1. Constructing a 
simconnect instance emulating protocol 0x2 will systematically throw an
UnsupportedOperationException exception when calling this methods.
	
	
### Data definitions
	
A major feature used by C/C++/C# implementation of simconnect client
libraries is the ability to directly cast a memory block to an
user-defined structure. This is technically impossible to do in Java
since memory layout of classes is not accessible.
Consequently the wrapping/unwrapping steps should be done by the user.
The base class `RecvSimObjectData` provides various methods for reading
primitives types and FS-specified compounds types from packets.

The general guidelines for transforming C/C++ applications looks like:

```c	
// ---------- C
struct MyStruct {
    int a;
    float b;
    int c;
};

// ...
struct MyStruct *pS;
pS = (struct MyStruct *) &pData->dwData;

printf("%d %f\n", pS->a, ps->b);		
// ....
```	
	
```java
// ---------- Java
class MyClass {
    int a;
    float b;
    int c;
};


// ...
MyClass mc = new MyClass();
mc.a = recvPacket.getInt32();
mc.b = recvPacket.getFloat32();
mc.c = recvPacket.getInt32();

System.out.println(mc.a + " " + mc.b);
// ...	
```
	
Compound data (like `InitPosition`, `Waypoint` or `LatLonAlt`) can also be
read directly.
The counterpart for writing complex data (used by `SetDataOnSimObject` 
or `SetClientData`) is the `DataWrapper` class.
	
	
### Reading responses
	
The base method for reading responses sent by the server is the 
callDispatch(Dispatcher) method of the SimConnect class.

Since the Dispatcher interface receives unparsed data from network
buffers, you should not directly implement this method yourself.
Instead two different implementations are available, the AbstractDispatcher
(C - like) and `DispatcherTask` (listeners).

1. AbstractDispatcher is an abstract class that reads received data
and construct the appropriate Recv* class. A subclass of `AbstractDispatcher`
must implement `onDispatch()` and generally proceeds to a large switch-block
with different IDs:

```java
class MyApp extends AbstractDispatcher {

        public void onDispatch(SimConnect simConnect, RecvPacket recv)  {
            switch (recv->getID()) {
            
                case RecvID.ID_EXCEPTION:
                    RecvException re = (RecvException) recv;
                    // ...
                
                case RecvID.ID_EVENT:
                    RecvEvent event = (RecvEvent) recv;
                    // ...
                
            }
        }
}
```
	
2. `DispatcherTask` has a list of listeners for each packet type and follows
the general event-subscription pattern of Java. One should provides a class
implementing an Handler interface and subscribe it to the appropriate
`Dispatcher`.
	
```java
DispatcherTask dispatcher;
dispatcher.addEventHandler(new EventHandler() {
    public void handleEvent(RecvEvent re) {
        System.out.println("Received Event [" + re.getEventID() + "]");
    }
});
simconnect.callDispatch(dispatcher);
```

`DispatcherTask` can be passed to the simconnect instance to parse each meassage
separately, but it can also be run in its own thread; e.g. :

```java
DispatcherTask dispatcher = new DispatcherTask(simconnect);
dispatcher.addEventHandler(...);
dispatcher.addXXXHandler(...);
dispatcher.createThread().start();
```
	
All handlers will be run from the dispatcher thread. Note that the 
`DispatcherTask` implements Runnable, so you can use your thread creation
method
	
	

	
