package dyvil.lang;

public abstract class Double implements Number
{
	protected double	value;
	
	@Override
	public abstract Double $eq(byte v);
	
	@Override
	public abstract Double $eq(short v);
	
	@Override
	public abstract Double $eq(char v);
	
	@Override
	public abstract Double $eq(int v);
	
	@Override
	public abstract Double $eq(long v);
	
	@Override
	public abstract Double $eq(float v);
	
	@Override
	public abstract Double $eq(double v);
	
	protected Double(double value)
	{
		this.value = value;
	}
	
	@Override
	public byte byteValue()
	{
		return (byte) this.value;
	}
	
	@Override
	public short shortValue()
	{
		return (short) this.value;
	}
	
	@Override
	public char charValue()
	{
		return (char) this.value;
	}
	
	@Override
	public int intValue()
	{
		return (int) this.value;
	}
	
	@Override
	public long longValue()
	{
		return (long) this.value;
	}
	
	@Override
	public float floatValue()
	{
		return (float) this.value;
	}
	
	@Override
	public double doubleValue()
	{
		return this.value;
	}
	
	// Unary operators
	
	@Override
	public Double $minus()
	{
		return this.$eq(-this.value);
	}
	
	@Override
	public Double $tilde()
	{
		// Unsupported
		return this;
	}
	
	@Override
	public Double $plus$plus()
	{
		return this.$eq(this.value + 1);
	}
	
	@Override
	public Double $minus$minus()
	{
		return this.$eq(this.value - 1);
	}
	
	@Override
	public Double sqr()
	{
		return this.$eq(this.value * this.value);
	}
	
	@Override
	public Double sqrt()
	{
		return this.$eq(1 / this.value);
	}
	
	// byte operators
	
	@Override
	public boolean $eq$eq(byte b)
	{
		return this.value == b;
	}
	
	@Override
	public boolean $bang$eq(byte b)
	{
		return this.value != b;
	}
	
	@Override
	public boolean $less(byte b)
	{
		return this.value < b;
	}
	
	@Override
	public boolean $less$eq(byte b)
	{
		return this.value == b;
	}
	
	@Override
	public boolean $greater(byte b)
	{
		return this.value > b;
	}
	
	@Override
	public boolean $greater$eq(byte b)
	{
		return this.value >= b;
	}
	
	@Override
	public Double $plus(byte v)
	{
		return this.$eq(this.value + v);
	}
	
	@Override
	public Double $minus(byte v)
	{
		return this.$eq(this.value - v);
	}
	
	@Override
	public Double $times(byte v)
	{
		return this.$eq(this.value * v);
	}
	
	@Override
	public Double $div(byte v)
	{
		return this.$eq(this.value / v);
	}
	
	@Override
	public Double $percent(byte v)
	{
		return this.$eq(this.value % v);
	}
	
	@Override
	public Double $bar(byte v)
	{
		return this;
	}
	
	@Override
	public Double $amp(byte v)
	{
		return this;
	}
	
	@Override
	public Double $up(byte v)
	{
		return this;
	}
	
	@Override
	public Double $less$less(byte v)
	{
		return this;
	}
	
	@Override
	public Double $greater$greater(byte v)
	{
		return this;
	}
	
	@Override
	public Double $greater$greater$greater(byte v)
	{
		return this;
	}
	
	// short operators
	
	@Override
	public boolean $eq$eq(short b)
	{
		return this.value == b;
	}
	
	@Override
	public boolean $bang$eq(short b)
	{
		return this.value != b;
	}
	
	@Override
	public boolean $less(short b)
	{
		return this.value < b;
	}
	
	@Override
	public boolean $less$eq(short b)
	{
		return this.value == b;
	}
	
	@Override
	public boolean $greater(short b)
	{
		return this.value > b;
	}
	
	@Override
	public boolean $greater$eq(short b)
	{
		return this.value >= b;
	}
	
	@Override
	public Double $plus(short v)
	{
		return this.$eq(this.value + v);
	}
	
	@Override
	public Double $minus(short v)
	{
		return this.$eq(this.value - v);
	}
	
	@Override
	public Double $times(short v)
	{
		return this.$eq(this.value * v);
	}
	
	@Override
	public Double $div(short v)
	{
		return this.$eq(this.value / v);
	}
	
	@Override
	public Double $percent(short v)
	{
		return this.$eq(this.value % v);
	}
	
	@Override
	public Double $bar(short v)
	{
		return this;
	}
	
	@Override
	public Double $amp(short v)
	{
		return this;
	}
	
	@Override
	public Double $up(short v)
	{
		return this;
	}
	
	@Override
	public Double $less$less(short v)
	{
		return this;
	}
	
	@Override
	public Double $greater$greater(short v)
	{
		return this;
	}
	
	@Override
	public Double $greater$greater$greater(short v)
	{
		return this;
	}
	
	// char operators
	
	@Override
	public boolean $eq$eq(char b)
	{
		return this.value == b;
	}
	
	@Override
	public boolean $bang$eq(char b)
	{
		return this.value != b;
	}
	
	@Override
	public boolean $less(char b)
	{
		return this.value < b;
	}
	
	@Override
	public boolean $less$eq(char b)
	{
		return this.value == b;
	}
	
	@Override
	public boolean $greater(char b)
	{
		return this.value > b;
	}
	
	@Override
	public boolean $greater$eq(char b)
	{
		return this.value >= b;
	}
	
	@Override
	public Double $plus(char v)
	{
		return this.$eq(this.value + v);
	}
	
	@Override
	public Double $minus(char v)
	{
		return this.$eq(this.value - v);
	}
	
	@Override
	public Double $times(char v)
	{
		return this.$eq(this.value * v);
	}
	
	@Override
	public Double $div(char v)
	{
		return this.$eq(this.value / v);
	}
	
	@Override
	public Double $percent(char v)
	{
		return this.$eq(this.value % v);
	}
	
	@Override
	public Double $bar(char v)
	{
		return this;
	}
	
	@Override
	public Double $amp(char v)
	{
		return this;
	}
	
	@Override
	public Double $up(char v)
	{
		return this;
	}
	
	@Override
	public Double $less$less(char v)
	{
		return this;
	}
	
	@Override
	public Double $greater$greater(char v)
	{
		return this;
	}
	
	@Override
	public Double $greater$greater$greater(char v)
	{
		return this;
	}
	
	// int operators
	
	@Override
	public boolean $eq$eq(int b)
	{
		return this.value == b;
	}
	
	@Override
	public boolean $bang$eq(int b)
	{
		return this.value != b;
	}
	
	@Override
	public boolean $less(int b)
	{
		return this.value < b;
	}
	
	@Override
	public boolean $less$eq(int b)
	{
		return this.value == b;
	}
	
	@Override
	public boolean $greater(int b)
	{
		return this.value > b;
	}
	
	@Override
	public boolean $greater$eq(int b)
	{
		return this.value >= b;
	}
	
	@Override
	public Double $plus(int v)
	{
		return this.$eq(this.value + v);
	}
	
	@Override
	public Double $minus(int v)
	{
		return this.$eq(this.value - v);
	}
	
	@Override
	public Double $times(int v)
	{
		return this.$eq(this.value * v);
	}
	
	@Override
	public Double $div(int v)
	{
		return this.$eq(this.value / v);
	}
	
	@Override
	public Double $percent(int v)
	{
		return this.$eq(this.value % v);
	}
	
	@Override
	public Double $bar(int v)
	{
		return this;
	}
	
	@Override
	public Double $amp(int v)
	{
		return this;
	}
	
	@Override
	public Double $up(int v)
	{
		return this;
	}
	
	@Override
	public Double $less$less(int v)
	{
		return this;
	}
	
	@Override
	public Double $greater$greater(int v)
	{
		return this;
	}
	
	@Override
	public Double $greater$greater$greater(int v)
	{
		return this;
	}
	
	// long operators
	
	@Override
	public boolean $eq$eq(long b)
	{
		return this.value == b;
	}
	
	@Override
	public boolean $bang$eq(long b)
	{
		return this.value != b;
	}
	
	@Override
	public boolean $less(long b)
	{
		return this.value < b;
	}
	
	@Override
	public boolean $less$eq(long b)
	{
		return this.value == b;
	}
	
	@Override
	public boolean $greater(long b)
	{
		return this.value > b;
	}
	
	@Override
	public boolean $greater$eq(long b)
	{
		return this.value >= b;
	}
	
	@Override
	public Double $plus(long v)
	{
		return this.$eq(this.value + v);
	}
	
	@Override
	public Double $minus(long v)
	{
		return this.$eq(this.value - v);
	}
	
	@Override
	public Double $times(long v)
	{
		return this.$eq(this.value * v);
	}
	
	@Override
	public Double $div(long v)
	{
		return this.$eq(this.value / v);
	}
	
	@Override
	public Double $percent(long v)
	{
		return this.$eq(this.value % v);
	}
	
	@Override
	public Double $bar(long v)
	{
		return this;
	}
	
	@Override
	public Double $amp(long v)
	{
		return this;
	}
	
	@Override
	public Double $up(long v)
	{
		return this;
	}
	
	@Override
	public Double $less$less(long v)
	{
		return this;
	}
	
	@Override
	public Double $greater$greater(long v)
	{
		return this;
	}
	
	@Override
	public Double $greater$greater$greater(long v)
	{
		return this;
	}
	
	// float operators
	
	@Override
	public boolean $eq$eq(float b)
	{
		return this.value == b;
	}
	
	@Override
	public boolean $bang$eq(float b)
	{
		return this.value != b;
	}
	
	@Override
	public boolean $less(float b)
	{
		return this.value < b;
	}
	
	@Override
	public boolean $less$eq(float b)
	{
		return this.value == b;
	}
	
	@Override
	public boolean $greater(float b)
	{
		return this.value > b;
	}
	
	@Override
	public boolean $greater$eq(float b)
	{
		return this.value >= b;
	}
	
	@Override
	public Double $plus(float v)
	{
		return this.$eq(this.value + v);
	}
	
	@Override
	public Double $minus(float v)
	{
		return this.$eq(this.value - v);
	}
	
	@Override
	public Double $times(float v)
	{
		return this.$eq(this.value * v);
	}
	
	@Override
	public Double $div(float v)
	{
		return this.$eq(this.value / v);
	}
	
	@Override
	public Double $percent(float v)
	{
		return this.$eq(this.value % v);
	}
	
	// double operators
	
	@Override
	public boolean $eq$eq(double b)
	{
		return this.value == b;
	}
	
	@Override
	public boolean $bang$eq(double b)
	{
		return this.value != b;
	}
	
	@Override
	public boolean $less(double b)
	{
		return this.value < b;
	}
	
	@Override
	public boolean $less$eq(double b)
	{
		return this.value == b;
	}
	
	@Override
	public boolean $greater(double b)
	{
		return this.value > b;
	}
	
	@Override
	public boolean $greater$eq(double b)
	{
		return this.value >= b;
	}
	
	@Override
	public Double $plus(double v)
	{
		return this.$eq(this.value + v);
	}
	
	@Override
	public Double $minus(double v)
	{
		return this.$eq(this.value - v);
	}
	
	@Override
	public Double $times(double v)
	{
		return this.$eq(this.value * v);
	}
	
	@Override
	public Double $div(double v)
	{
		return this.$eq(this.value / v);
	}
	
	@Override
	public Double $percent(double v)
	{
		return this.$eq(this.value % v);
	}
}
