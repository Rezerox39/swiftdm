import { useEffect } from 'react';
import Aurora from '../reactbits/Aurora';
import BlurText from '../reactbits/BlurText';
import Magnet from '../reactbits/Magnet';
import { useArmzStore } from '../store/useArmzStore';

interface LaunchScreenProps {
  onFinished: () => void;
}

export function LaunchScreen({ onFinished }: LaunchScreenProps) {
  const completeIntro = useArmzStore((state) => state.completeIntro);
  const haptic = useArmzStore((state) => state.haptic);

  useEffect(() => {
    const timer = window.setTimeout(() => completeIntro(), 1200);
    return () => window.clearTimeout(timer);
  }, [completeIntro]);

  const skip = () => {
    completeIntro();
    haptic('light');
    onFinished();
  };

  return (
    <section className="relative h-full w-full overflow-hidden bg-black">
      <Aurora
        className="absolute inset-0 h-full w-full"
        colorStops={['#0A84FF', '#5E5CE6', '#0A84FF']}
        amplitude={0.7}
        blend={0.55}
        speed={0.9}
        height={0.65}
        glow={0.65}
        brightness={0.9}
        layers={2}
        lightMode={false}
        paused={false}
        dpr={1.5}
      />
      <div className="absolute inset-0 bg-black/35" />
      <div className="relative z-10 flex h-full flex-col items-center justify-center gap-6 px-8">
        <BlurText
          text="ARMz"
          className="text-[44px] font-bold tracking-tight text-white"
          animateBy="words"
          direction="top"
          effect="blur"
          blur={8}
          distance={24}
          staggerFrom="start"
          trigger="load"
          color="transparent"
          fromColor="rgba(255,255,255,0.28)"
          easing="easeOut"
        />
        <div className="h-20 w-20" aria-hidden="true">
          <svg viewBox="0 0 64 64" className="h-full w-full">
            <path d="M12 44 L32 14 L52 44" fill="none" stroke="white" strokeWidth="6" strokeLinecap="round" strokeLinejoin="round" pathLength={1} />
            <path d="M22 44 L32 34 L42 44" fill="none" stroke="#0A84FF" strokeWidth="6" strokeLinecap="round" strokeLinejoin="round" pathLength={1} />
          </svg>
        </div>
        <Magnet padding={40}>
          <button
            type="button"
            onClick={skip}
            className="inline-flex h-12 min-w-[180px] items-center justify-center rounded-full bg-white px-8 text-[15px] font-semibold text-black"
          >
            Continue
          </button>
        </Magnet>
      </div>
    </section>
  );
}
