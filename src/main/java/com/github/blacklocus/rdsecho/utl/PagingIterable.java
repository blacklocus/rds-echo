package com.github.blacklocus.rdsecho.utl;

import com.google.common.base.Supplier;
import org.apache.http.annotation.NotThreadSafe;

import java.util.Iterator;

@NotThreadSafe
class PagingIterable<T> implements Iterable<T>, Iterator<T> {

    final Supplier<Iterable<T>> supplier;
    Iterator<T> currentPage;

    public PagingIterable(Supplier<Iterable<T>> supplier) {
        this.supplier = supplier;
        this.currentPage = supplier.get().iterator();
    }

    @Override
    public Iterator<T> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        boolean x = currentPage.hasNext();
        return x;
    }

    @Override
    public T next() {
        T next = currentPage.next();
        if (!currentPage.hasNext()) {
            currentPage = supplier.get().iterator();
        }
        return next;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
