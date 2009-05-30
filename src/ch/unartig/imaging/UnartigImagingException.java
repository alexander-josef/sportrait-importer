package ch.unartig.imaging;


public class UnartigImagingException extends Exception
{
    public UnartigImagingException(String message, Throwable throwable)
    {
        super(message,throwable);
    }
    public UnartigImagingException(String message)
    {
        super(message);
    }
}