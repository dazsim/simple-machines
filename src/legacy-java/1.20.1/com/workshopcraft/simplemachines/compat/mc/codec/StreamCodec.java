package com.workshopcraft.simplemachines.compat.mc.codec;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.BiFunction;
import java.util.function.Function;

/** Stand-in for the 1.20.5+ StreamCodec over FriendlyByteBuf; B is kept only for source compatibility. */
public interface StreamCodec<B, V> {
    V decode(FriendlyByteBuf buf);

    void encode(FriendlyByteBuf buf, V value);

    static <B, V> StreamCodec<B, V> of(Function<FriendlyByteBuf, V> dec, java.util.function.BiConsumer<FriendlyByteBuf, V> enc) {
        return new StreamCodec<>() {
            public V decode(FriendlyByteBuf buf) {
                return dec.apply(buf);
            }

            public void encode(FriendlyByteBuf buf, V value) {
                enc.accept(buf, value);
            }
        };
    }

    default <O> StreamCodec<B, O> map(Function<? super V, ? extends O> to, Function<? super O, ? extends V> from) {
        return of(b -> to.apply(decode(b)), (b, v) -> encode(b, from.apply(v)));
    }

    @SuppressWarnings("unchecked")
    default <S> StreamCodec<S, V> cast() {
        return (StreamCodec<S, V>) this;
    }

    default <O> O apply(Function<StreamCodec<B, V>, O> op) {
        return op.apply(this);
    }

    static <B, V> StreamCodec<B, V> unit(V v) {
        return of(b -> v, (b, x) -> {
        });
    }

    static <B, C, T1> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, Function<T1, C> f) {
        return of(b -> f.apply(c1.decode(b)), (b, v) -> c1.encode(b, g1.apply(v)));
    }

    static <B, C, T1, T2> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2,
                                                      BiFunction<T1, T2, C> f) {
        return of(b -> f.apply(c1.decode(b), c2.decode(b)), (b, v) -> {
            c1.encode(b, g1.apply(v));
            c2.encode(b, g2.apply(v));
        });
    }

    static <B, C, T1, T2, T3> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2,
                                                          StreamCodec<? super B, T3> c3, Function<C, T3> g3, Function3<T1, T2, T3, C> f) {
        return of(b -> f.apply(c1.decode(b), c2.decode(b), c3.decode(b)), (b, v) -> {
            c1.encode(b, g1.apply(v));
            c2.encode(b, g2.apply(v));
            c3.encode(b, g3.apply(v));
        });
    }

    static <B, C, T1, T2, T3, T4> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2,
                                                              StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4,
                                                              Function4<T1, T2, T3, T4, C> f) {
        return of(b -> f.apply(c1.decode(b), c2.decode(b), c3.decode(b), c4.decode(b)), (b, v) -> {
            c1.encode(b, g1.apply(v));
            c2.encode(b, g2.apply(v));
            c3.encode(b, g3.apply(v));
            c4.encode(b, g4.apply(v));
        });
    }

    static <B, C, T1, T2, T3, T4, T5> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2,
                                                                  StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4,
                                                                  StreamCodec<? super B, T5> c5, Function<C, T5> g5, Function5<T1, T2, T3, T4, T5, C> f) {
        return of(b -> f.apply(c1.decode(b), c2.decode(b), c3.decode(b), c4.decode(b), c5.decode(b)), (b, v) -> {
            c1.encode(b, g1.apply(v));
            c2.encode(b, g2.apply(v));
            c3.encode(b, g3.apply(v));
            c4.encode(b, g4.apply(v));
            c5.encode(b, g5.apply(v));
        });
    }

    static <B, C, T1, T2, T3, T4, T5, T6> StreamCodec<B, C> composite(StreamCodec<? super B, T1> c1, Function<C, T1> g1, StreamCodec<? super B, T2> c2, Function<C, T2> g2,
                                                                      StreamCodec<? super B, T3> c3, Function<C, T3> g3, StreamCodec<? super B, T4> c4, Function<C, T4> g4,
                                                                      StreamCodec<? super B, T5> c5, Function<C, T5> g5, StreamCodec<? super B, T6> c6, Function<C, T6> g6,
                                                                      Function6<T1, T2, T3, T4, T5, T6, C> f) {
        return of(b -> f.apply(c1.decode(b), c2.decode(b), c3.decode(b), c4.decode(b), c5.decode(b), c6.decode(b)), (b, v) -> {
            c1.encode(b, g1.apply(v));
            c2.encode(b, g2.apply(v));
            c3.encode(b, g3.apply(v));
            c4.encode(b, g4.apply(v));
            c5.encode(b, g5.apply(v));
            c6.encode(b, g6.apply(v));
        });
    }
}
