package model;

public class TimeAllowance {
    private int allocated;
    private int used;
    private int reserved;

    public TimeAllowance(int allocated) 
    {
        this(allocated, 0, 0);
    }

    public TimeAllowance(int allocated, int used, int reserved) 
    {
        this.allocated = allocated;
        this.used = used;
        this.reserved = reserved;
    }

    public int getAllocated() 
    {
        return allocated;
    }

    public int getUsed() 
    {
        return used;
    }

    public int getReserved() 
    {
        return reserved;
    }

    public int available() 
    {
        return allocated - used - reserved;
    }

    public void reserve(int minutes) throws RuleException
{
    if (minutes <= 0)
    {
        throw new RuleException("A job duration must be a positive number of minutes.");
    }
    if (minutes > available())
    {
        throw new RuleException("This job needs " + minutes
                + " min, but you have only " + available() + " min available.");
    }
    reserved += minutes;
}

    public void release(int minutes) throws RuleException 
    {
        if (minutes < 0 || minutes > reserved) 
            {
                throw new RuleException("Cannot release " + minutes
                + " min; only " + reserved + " min are reserved.");
            }
        reserved -= minutes;
    }

    public void charge(int minutes) throws RuleException 
    {
        if (minutes < 0) 
            {
                throw new RuleException("A charge cannot be negative.");
            }
        used += minutes;
    }

    public void grant(int minutes) throws RuleException 
    {
        if (minutes <= 0) 
            {
                throw new RuleException("Extra time must be a positive whole number of minutes.");
            }
        allocated += minutes;
    }
}