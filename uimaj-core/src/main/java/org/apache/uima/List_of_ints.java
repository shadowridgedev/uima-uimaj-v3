/**
 * 
 */
package org.apache.uima;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.uima.internal.util.IntListIterator;

/**
 * a List API that returns ints instead of T
 */
public interface List_of_ints extends Iterable<Integer> {

  /* (non-Javadoc)
   * @see java.util.List#size()
   */
  public int size();

  /* (non-Javadoc)
   * @see java.util.List#isEmpty()
   */
  default boolean isEmpty() {
    return size() == 0;
  };

  /* (non-Javadoc)
   * @see java.util.List#contains(java.lang.Object)
   */
  default boolean contains(int i) {
    return indexOf(i) != -1;
  }

  /* (non-Javadoc)
   * @see java.util.List#toArray()
   */
  public int[] toArray();
  
  /**
   * Avoid copying, return the original array, if start/end offsets not in use
   * @return -
   */
  public int[] toArrayMinCopy();

  /* (non-Javadoc)
   * @see java.util.List#add(java.lang.Object)
   */
  public boolean add(int i);

  /* (non-Javadoc)
   * @see java.util.List#remove(java.lang.Object)
   */
  public boolean remove(int i);

  /* (non-Javadoc)
   * @see java.util.List#clear()
   */
  public void clear();

  /* (non-Javadoc)
   * @see java.util.List#get(int)
   */
  public int get(int index);

  /* (non-Javadoc)
   * @see java.util.List#set(int, java.lang.Object)
   */
  public int set(int index, int element);

  /* (non-Javadoc)
   * @see java.util.List#add(int, java.lang.Object)
   */
  public void add(int index, int element);

  /* (non-Javadoc)
   * @see java.util.List#remove(int)
   */
  public int removeAtIndex(int index);

  /* (non-Javadoc)
   * @see java.util.List#indexOf(java.lang.Object)
   */
  public int indexOf(int i);

  /* (non-Javadoc)
   * @see java.util.List#lastIndexOf(java.lang.Object)
   */
  public int lastIndexOf(int i);

  public List_of_ints subList(int fromIndex, int toIndex);
  
  public Iterator<Integer> iterator();
  
  public IntListIterator intListIterator();
  
  public void copyFromArray(int[] src, int srcPos, int destPos, int length);
  
  public void copyToArray(int srcPos, int[] dest, int destPos, int length);

  public void sort();
  
  public static int[] EMPTY_INT_ARRAY = new int[0];

  
  public static List_of_ints EMPTY_LIST() {
    return new List_of_ints() {
      
      @Override
      public int[] toArray() {
        return EMPTY_INT_ARRAY;
      }
      
      @Override
      public int[] toArrayMinCopy() {
        return EMPTY_INT_ARRAY;        
      }
      
      @Override
      public List_of_ints subList(int fromIndex, int toIndex) {
        throw new IndexOutOfBoundsException();
      }
      
      @Override
      public int size() {
        return 0;
      }
      
      @Override
      public int set(int index, int element) {
        throw new IndexOutOfBoundsException();
      }
      
      @Override
      public int removeAtIndex(int index) {
        throw new IndexOutOfBoundsException();
      }
      
      @Override
      public boolean remove(int i) {
        return false;
      }
      
      @Override
      public int lastIndexOf(int i) {
        return -1;
      }
            
      @Override
      public int indexOf(int i) {
        return -1;
      }
      
      @Override
      public int get(int index) {
        throw new IndexOutOfBoundsException();
      }
      
      @Override
      public boolean contains(int i) {
        return false;
      }
      
      @Override
      public void clear() {        
      }
      
      @Override
      public void add(int index, int element) {
        throw new UnsupportedOperationException();
      }
      
      @Override
      public boolean add(int i) {
        throw new UnsupportedOperationException();
      }

      @Override
      public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
          @Override
          public boolean hasNext() {return false;}
          @Override
          public Integer next() {throw new NoSuchElementException();}
        };
      }
      
      @Override
      public IntListIterator intListIterator() {
        return new IntListIterator() {
          @Override
          public boolean hasNext() {return false;}
          @Override
          public int next() throws NoSuchElementException {throw new NoSuchElementException();}
          @Override
          public boolean hasPrevious() {return false;}
          @Override
          public int previous() {throw new NoSuchElementException();}
          @Override
          public void moveToStart() {}
          @Override
          public void moveToEnd() {}
        };
      }
      
      @Override
      public void copyFromArray(int[] src, int srcPos, int destPos, int length) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void copyToArray(int srcPos, int[] dest, int destPos, int length) {
        throw new UnsupportedOperationException();
      }
      
      @Override
      public void sort() {};
    };
  }
  
  static List_of_ints newInstance(int[] ia) {
    return newInstance(ia, 0, ia.length);
  }
  
  static List_of_ints newInstance(final int[] ia, final int start, final int end) {
    return new List_of_ints() {

      @Override
      public int size() {
        return end - start;
      }

      @Override
      public int[] toArray() {
        return Arrays.copyOfRange(ia, start, end); 
      }
      
      @Override
      public int[] toArrayMinCopy() {
        return (start == 0 && end == size()) 
            ? ia
            : toArray();
      }

      @Override
      public boolean add(int i) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean remove(int i) {
        throw new UnsupportedOperationException();
      }

      @Override
      public void clear() {
        throw new UnsupportedOperationException();
      }

      @Override
      public int get(int index) {
        return ia[start + index];
      }

      @Override
      public int set(int index, int element) {
        int r = get(start + index);
        ia[start + index] = element;
        return r;
      }

      @Override
      public void add(int index, int element) {
        throw new UnsupportedOperationException();
      }

      @Override
      public int removeAtIndex(int index) {
        throw new UnsupportedOperationException();
      }

      @Override
      public int indexOf(int e) {
        for (int i = start; i < end; i++) {
          if (e == ia[i]) {
            return i;
          }
        }
        return -1;
      }

      @Override
      public int lastIndexOf(int e) {
        for (int i = end - 1; i >= start; i--) {
          if (e == ia[i]) {
            return i;
          }
        }
        return -1;
      }

      @Override
      public List_of_ints subList(int fromIndex, int toIndex) {
        return List_of_ints.newInstance(ia, start + fromIndex, start + toIndex);
      }

      @Override
      public Iterator<Integer> iterator() {
        return new Iterator<Integer> () {
          int pos = 0;
          @Override
          public boolean hasNext() {
            return pos < ia.length;
          }

          @Override
          public Integer next() {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            return ia[pos++];
          }
       
        };
      }  
      
      @Override 
      public IntListIterator intListIterator() {
        return new IntListIterator() {

          private int pos = 0;
          @Override
          public boolean hasNext() {
            return pos >= 0 && pos < size();
          }

          @Override
          public int next() throws NoSuchElementException {
            if (!hasNext()) {
              throw new NoSuchElementException();
            }
            return get(pos++);
          }

          @Override
          public boolean hasPrevious() {
            return pos >= 0 && pos < size();  // same as has next
          }

          @Override
          public int previous() {
            if (!hasPrevious()) {
              throw new NoSuchElementException();
            }
            return get(pos--);
          }

          @Override
          public void moveToStart() {
            pos = 0;
          }

          @Override
          public void moveToEnd() {
            pos = size() - 1;
          }
          
        };
      }
      
      @Override
      public void copyFromArray(int[] src, int srcPos, int destPos, int length) {
        System.arraycopy(src, srcPos, ia, start + destPos, length);
      }
      
      @Override
      public void copyToArray(int srcPos, int[] dest, int destPos, int length) {
        System.arraycopy(ia, start + srcPos, dest, destPos, length);
      }
      
      @Override
      public void sort() {
        Arrays.sort(ia, start, end);
      }
    
    };
  }

}
