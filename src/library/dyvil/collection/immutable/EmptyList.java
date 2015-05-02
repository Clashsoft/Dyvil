package dyvil.collection.immutable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import dyvil.array.ObjectArray;
import dyvil.collection.EmptyIterator;
import dyvil.collection.mutable.MutableList;
import dyvil.lang.Collection;

public class EmptyList<E> implements ImmutableList<E>
{
	static final EmptyList	emptyList	= new EmptyList();
	
	public static <E> EmptyList<E> apply()
	{
		return emptyList;
	}
	
	private EmptyList()
	{
	}
	
	@Override
	public int size()
	{
		return 0;
	}
	
	@Override
	public boolean isEmpty()
	{
		return true;
	}
	
	@Override
	public Iterator<E> iterator()
	{
		return EmptyIterator.apply();
	}
	
	@Override
	public Spliterator<E> spliterator()
	{
		return Spliterators.emptySpliterator();
	}
	
	@Override
	public void forEach(Consumer<? super E> action)
	{
	}
	
	@Override
	public boolean $qmark(Object element)
	{
		return false;
	}
	
	@Override
	public E apply(int index)
	{
		throw new IndexOutOfBoundsException("Empty List.apply()");
	}
	
	@Override
	public E get(int index)
	{
		return null;
	}
	
	@Override
	public ImmutableList<E> slice(int startIndex, int length)
	{
		if (startIndex > 0 || length > 0)
		{
			throw new IndexOutOfBoundsException("Empty List Slice out of range");
		}
		return this;
	}
	
	@Override
	public int indexOf(E element)
	{
		return -1;
	}
	
	@Override
	public int lastIndexOf(E element)
	{
		return -1;
	}
	
	@Override
	public ImmutableList<E> $plus(E element)
	{
		return new SingletonList(element);
	}
	
	@Override
	public ImmutableList<? extends E> $plus$plus(Collection<? extends E> collection)
	{
		return (ImmutableList<? extends E>) collection.immutable();
	}
	
	@Override
	public ImmutableList<E> $minus(E element)
	{
		return this;
	}
	
	@Override
	public ImmutableList<? extends E> $minus$minus(Collection<? extends E> collection)
	{
		return this;
	}
	
	@Override
	public ImmutableList<? extends E> $amp(Collection<? extends E> collection)
	{
		return this;
	}
	
	@Override
	public <R> ImmutableList<R> mapped(Function<? super E, ? extends R> mapper)
	{
		return (ImmutableList<R>) this;
	}
	
	@Override
	public <R> ImmutableList<R> flatMapped(Function<? super E, ? extends Iterable<? extends R>> mapper)
	{
		return (ImmutableList<R>) this;
	}
	
	@Override
	public ImmutableList<E> filtered(Predicate<? super E> condition)
	{
		return this;
	}
	
	@Override
	public ImmutableList<E> sorted()
	{
		return this;
	}
	
	@Override
	public ImmutableList<E> sorted(Comparator<? super E> comparator)
	{
		return this;
	}
	
	@Override
	public Object[] toArray()
	{
		return ObjectArray.EMPTY;
	}
	
	@Override
	public Object[] toArray(Object[] store)
	{
		return store;
	}
	
	@Override
	public E[] toArray(Class<E> type)
	{
		return (E[]) ObjectArray.EMPTY;
	}
	
	@Override
	public ImmutableList<E> copy()
	{
		return this;
	}
	
	@Override
	public MutableList<E> mutable()
	{
		return MutableList.apply();
	}
	
	@Override
	public String toString()
	{
		return "[]";
	}
}
