/*==============================================================================
 * Copyright 2022 IBM Corporation and others.
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *=============================================================================*/
package org.apache.yoko.util.concurrent;

final class StrongNode<T> implements VNode<T> {
    private final T value;
    private PNode<T> prev;
    private NNode<T> next;
    
    StrongNode(T value) {this.value = value;}
    public PNode<T> prev() {return prev;}
    public NNode<T> next() {return next;}
    public void prev(PNode<T> pnode) {prev = pnode;}
    public void next(NNode<T> nnode) {next = nnode;}
    public T get() {return value;}

    public void insertAfter(PNode<T> pnode) {
        NNode<T> nnode = pnode.next();
        this.next = nnode;
        this.prev = pnode;
        nnode.prev(this);
        pnode.next(this);
    }

    public void delete() {
        this.prev.next(this.next);
        this.next.prev(this.prev);
        this.prev = null;
        this.next = null;
    }
}
