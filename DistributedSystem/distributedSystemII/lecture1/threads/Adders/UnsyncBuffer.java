public class UnsyncBuffer
{
    private int sharedInt = 0;

    public void incSharedInt()
    {
	sharedInt += 1;
    }

    public int getSharedInt()
    {
	return sharedInt;
    }
}
