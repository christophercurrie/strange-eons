package ca.cgjennings.math;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class MathTest {

    @Test
    void countsArrangements() {
        assertThat(Combinatorics.factorial(0)).isEqualTo(1);
        assertThat(Combinatorics.factorial(5)).isEqualTo(120);
        assertThat(Combinatorics.permutations(5, 2)).isEqualTo(20);
        assertThat(Combinatorics.combinations(5, 2)).isEqualTo(10);
        assertThat(Combinatorics.combinations(6, 0)).isEqualTo(1);
    }

    @Test
    void binomialDistributionSumsToOne() {
        double total = 0;
        for (int k = 0; k <= 6; ++k) {
            total += Combinatorics.probabilityMass(k, 6, 1.0 / 3.0);
        }
        assertThat(total).isCloseTo(1.0, within(1e-12));
        assertThat(Combinatorics.cumulativeDistribution(6, 6, 0.5)).isCloseTo(1.0, within(1e-12));
        assertThat(Combinatorics.cumulativeDistribution(2, 6, 0.5) + Combinatorics.upperCumulativeDistribution(3, 6, 0.5))
                .isCloseTo(1.0, within(1e-12));
    }

    @Test
    void interpolatesMapsAndClamps() {
        assertThat(Interpolation.lerp(0.25, 10.0, 20.0)).isEqualTo(12.5);
        assertThat(Interpolation.lerp(0.5f, 0f, 4f)).isEqualTo(2f);
        assertThat(Interpolation.lerp(0.5f, 0, 10)).isEqualTo(5);
        assertThat(Interpolation.map(5.0, 0.0, 10.0, 100.0, 200.0)).isEqualTo(150.0);
        assertThat(Interpolation.map(5f, 0f, 10f, 100f, 200f)).isEqualTo(150f);
        assertThat(Interpolation.map(5, 0, 10, 100, 200)).isEqualTo(150);
        assertThat(Interpolation.clamp(12.0, 0.0, 10.0)).isEqualTo(10.0);
        assertThat(Interpolation.clamp(-1f, 0f, 10f)).isEqualTo(0f);
        assertThat(Interpolation.clamp(5, 0, 10)).isEqualTo(5);
        assertThat(Interpolation.createLerpFunction(10, 20).f(0.5)).isEqualTo(15.0);
        assertThat(Interpolation.createMapFunction(0, 10, 100, 200).f(5)).isEqualTo(150.0);
    }

    @Test
    void rejectsImpossibleProbabilities() {
        assertThatThrownBy(() -> Combinatorics.probabilityMass(1, 2, 1.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Combinatorics.probabilityMass(1, 2, -0.1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapsDegenerateRangesAndClampsBelow() {
        assertThatThrownBy(() -> Interpolation.map(3.0, 2.0, 2.0, 10.0, 20.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Interpolation.createMapFunction(2, 2, 0, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Interpolation.map(3f, 2f, 2f, 10f, 20f)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Interpolation.map(3, 2, 2, 10, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThat(Interpolation.clamp(-2.0, 0.0, 1.0)).isZero();
        assertThat(Interpolation.clamp(2f, 0f, 1f)).isEqualTo(1f);
        assertThat(Interpolation.clamp(-5, 0, 10)).isZero();
        assertThat(Interpolation.clamp(15, 0, 10)).isEqualTo(10);
        assertThat(Fn.IDENTITY.f(4.5)).isEqualTo(4.5);
    }

    @Test
    void linearRegressionFitsALine() {
        Interpolation.LinearRegression fit = new Interpolation.LinearRegression(
                new double[]{0, 1, 2, 3}, new double[]{1, 3, 5, 7});
        assertThat(fit.f(4)).isCloseTo(9, within(1e-9));
        assertThat(fit.getNumCoefficients()).isEqualTo(2);
        assertThat(fit.getCoefficient(0)).isCloseTo(1, within(1e-9));
        assertThat(fit.getCoefficient(1)).isCloseTo(2, within(1e-9));
        assertThatThrownBy(() -> fit.getCoefficient(2)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void quadraticRegressionFitsAParabola() {
        Interpolation.QuadraticRegression fit = new Interpolation.QuadraticRegression(
                new double[]{-2, -1, 0, 1, 2}, new double[]{4, 1, 0, 1, 4});
        assertThat(fit.f(3)).isCloseTo(9, within(1e-6));
        assertThat(fit.getNumCoefficients()).isEqualTo(3);
        assertThat(fit.getCoefficient(2)).isCloseTo(1, within(1e-6));
        assertThatThrownBy(() -> fit.getCoefficient(3)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void cubicSplinePassesThroughItsPoints() {
        double[] x = {0, 1, 2, 3};
        double[] y = {0, 1, 0, 1};
        Interpolation.CubicSpline natural = new Interpolation.CubicSpline(x, y);
        Interpolation.CubicSpline clamped = new Interpolation.CubicSpline(x, y, 0, 0);
        for (int i = 0; i < x.length; ++i) {
            assertThat(natural.f(x[i])).isCloseTo(y[i], within(1e-9));
            assertThat(clamped.f(x[i])).isCloseTo(y[i], within(1e-9));
        }
        assertThat(natural.f(1.5)).isBetween(0.0, 1.0);
    }

    @Test
    void interpolatorsValidatePoints() {
        assertThatThrownBy(() -> new Interpolation.LinearRegression(new double[]{0, 1}, new double[]{0}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Interpolation.CubicSpline(new double[]{1, 0}, new double[]{0, 1}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
