public class SyncBuffer 
{
    private int sharedInt = -0;

    public synchronized void incSharedInt()
    {
	sharedInt += 1;
    }

    public synchronized int getSharedInt()
    {
	return sharedInt;
    }
}

