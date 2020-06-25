public class AddersTest
{
    public static void main( String args[] )
    {
	//final UnsyncBuffer buffer = new UnsyncBuffer();
	final SyncBuffer buffer = new SyncBuffer();

	class Adder implements Runnable {
	    public void run() {
		for ( int count = 1; count <= 1000000; count++ )
		    buffer.incSharedInt();
		System.out.println(Thread.currentThread().getName() 
				   + " done; current buffer value is "
				   +  buffer.getSharedInt());
	    }};

	new Thread(new Adder(), "Adder 1").start();
	new Thread(new Adder(), "Adder 2").start();
    }
}
