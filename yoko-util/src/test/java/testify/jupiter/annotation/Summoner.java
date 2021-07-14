/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package testify.jupiter.annotation;

import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;
import static org.junit.platform.commons.support.AnnotationSupport.findRepeatableAnnotations;

/**
 * Summon the steward for an annotation or list of annotations.
 * @param <P> either the annotation type or a List of the annotation type
 *           (Note: the restrictions are introduced by the static factory methods on the interface)
 * @param <S> the steward type
 */
public interface Summoner<P, S> {
    interface StewardSummoner<P, S> {
        /** Summon the steward for a given annotation or list of annotations */
        S summon(P p);
        /* Summon the steward for the (already supplied) ExtensionContext */
        Optional<S> summon();
    }

    StewardSummoner<P, S> forContext(ExtensionContext context);

    static <A extends Annotation, S> Summoner<A, S> forAnnotation(Class<A> annotationType, Class<S> stewardType, Function<A, S> factory) {
        return new SummonerImpl<>(annotationType, stewardType, factory, null, SummonerImpl::lookupAnnotation);
    }

    static <A extends Annotation, S> Summoner<A, S> forAnnotation(Class<A> annotationType, Class<S> stewardType, BiFunction<A, ExtensionContext, S> factory) {
        return new SummonerImpl<>(annotationType, stewardType, factory, null, SummonerImpl::lookupAnnotation);
    }

    static <A extends Annotation, S> Summoner<List<A>, S> forRepeatableAnnotation(Class<A> annotationType, Class<S> stewardType, Function<List<A>, S> factory) {
        return new SummonerImpl<>(annotationType, stewardType, factory, null, SummonerImpl::lookupAnnotations);
    }
}

/**
 * This class implements both Summoner and StewardSummoner. The separate interfaces only exist to
 * restrict the order of invocation. The {@link Summoner#forContext(ExtensionContext)} method must be called
 * before the {@link StewardSummoner#summon()} or {@link StewardSummoner#summon(Object)} methods.
 *
 * @param <A> the annotation type
 * @param <P> the parameter type (either A or List of A)
 * @param <S> the steward type
 */
class SummonerImpl<A extends Annotation, P, S> implements Summoner<P, S>, Summoner.StewardSummoner<P, S> {
    private final Class<A> annotationType;
    private final Class<S> stewardType;
    private final BiFunction<P, ExtensionContext, S> factory;
    private final Function<SummonerImpl<A, P, S>, Optional<P>> finder;
    private final ExtensionContext context;
    private final Class<?> testClass;
    private final ExtensionContext.Store store;

    SummonerImpl(Class<A> annotationType, Class<S> stewardType, BiFunction<P, ExtensionContext, S> factory, ExtensionContext context, Function<SummonerImpl<A, P, S>, Optional<P>> finder) {
        this.annotationType = annotationType;
        this.stewardType = stewardType;
        this.factory = factory;
        this.finder = finder;
        this.context = context;
        this.testClass = context == null ? null : context.getRequiredTestClass();
        this.store = context == null ? null : context.getStore(ExtensionContext.Namespace.create(stewardType, "test annotation steward namespace"));
    }

    SummonerImpl(Class<A> annotationType, Class<S> stewardType, Function<P, S> factory, ExtensionContext context, Function<SummonerImpl<A, P, S>, Optional<P>> finder) {
        this(annotationType, stewardType, (p, c)  -> factory.apply(p), context, finder);
    }

        @Override
    public SummonerImpl<A, P, S> forContext(ExtensionContext context) { return new SummonerImpl<>(annotationType, stewardType, factory, context, finder); }

    public final S summon(P parameter) { return store.getOrComputeIfAbsent(parameter, p -> factory.apply(p, context), stewardType); } // curried function, yum!

    public final Optional<S> summon() { return finder.apply(this).map(this::summon); }

    Optional<A> lookupAnnotation() {
        final Optional<AnnotatedElement> element = context.getElement();
        Optional<A> result = element.flatMap(e -> findAnnotation(e, annotationType));
        if (result.isPresent() || element.filter(testClass::equals).isPresent()) return result;
        return findAnnotation(testClass, annotationType);
    }

    Optional<List<A>> lookupAnnotations() {
        final Optional<AnnotatedElement> element = context.getElement();
        Optional<List<A>> result = element.map(e -> findRepeatableAnnotations(e, annotationType)).filter(l -> l.size() > 0);
        if (result.isPresent() || element.filter(testClass::equals).isPresent()) return result;
        return Optional.of(findRepeatableAnnotations(testClass, annotationType)).filter(l -> l.size() > 0);
    }
}
