import { AfterViewInit, Component, ElementRef, Input, OnDestroy, ViewChild } from '@angular/core';
import type * as ThreeTypes from 'three';

@Component({
  selector: 'app-career-visual',
  standalone: true,
  template: '<canvas #canvas aria-hidden="true"></canvas>',
  styles: [`
    :host { position: absolute; inset: 0; display: block; pointer-events: none; }
    canvas { display: block; width: 100%; height: 100%; }
    :host(.auth-scene) { opacity: .5; }
    :host(.ambient-scene) { inset: auto 0 auto auto; top: -62px; width: min(34vw, 390px); height: 270px; opacity: .7; }
    :host(.report-scene) { inset: auto 0 auto auto; top: -56px; width: 190px; height: 150px; opacity: .38; }
  `],
})
export class CareerVisualComponent implements AfterViewInit, OnDestroy {
  @ViewChild('canvas', { static: true }) private canvas!: ElementRef<HTMLCanvasElement>;
  @Input() variant: 'hero' | 'auth' | 'ambient' | 'report' = 'hero';

  private renderer?: ThreeTypes.WebGLRenderer;
  private scene?: ThreeTypes.Scene;
  private camera?: ThreeTypes.PerspectiveCamera;
  private artwork?: ThreeTypes.Group;
  private resizeObserver?: ResizeObserver;
  private destroyed = false;
  private reducedMotion = false;

  ngAfterViewInit(): void {
    this.reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    void this.createScene();
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.resizeObserver?.disconnect();
    this.renderer?.setAnimationLoop(null);
    this.scene?.traverse((object) => {
      const mesh = object as ThreeTypes.Mesh;
      mesh.geometry?.dispose();
      const materials = Array.isArray(mesh.material) ? mesh.material : [mesh.material];
      materials?.forEach((material) => material?.dispose());
    });
    this.renderer?.dispose();
  }

  private async createScene(): Promise<void> {
    try {
      const THREE = await import('three');
      if (this.destroyed) return;

      const host = this.canvas.nativeElement.parentElement;
      if (!host) return;
      const bounds = host.getBoundingClientRect();
      const width = Math.max(bounds.width, 1);
      const height = Math.max(bounds.height, 1);
      const scene = new THREE.Scene();
      const camera = new THREE.PerspectiveCamera(34, width / height, 0.1, 80);
      camera.position.set(0, 0, 8.6);
      const renderer = new THREE.WebGLRenderer({ canvas: this.canvas.nativeElement, alpha: true, antialias: true });
      renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.5));
      renderer.setSize(width, height, false);
      renderer.outputColorSpace = THREE.SRGBColorSpace;

      const artwork = new THREE.Group();
      scene.add(artwork);
      scene.add(new THREE.AmbientLight(0xaec3ff, 1.8));
      const keyLight = new THREE.PointLight(0xffffff, 48, 18);
      keyLight.position.set(-3.2, 3.8, 5);
      scene.add(keyLight);
      const rimLight = new THREE.PointLight(0x715bff, 32, 15);
      rimLight.position.set(3.4, -2.5, -1);
      scene.add(rimLight);

      const authPalette = this.variant === 'auth';
      const reportPalette = this.variant === 'report';
      const coreColor = authPalette ? 0x3568cf : reportPalette ? 0x7057df : 0x315fe8;
      const core = new THREE.Mesh(
        new THREE.SphereGeometry(1.22, 64, 48),
        new THREE.MeshPhysicalMaterial({ color: coreColor, metalness: 0.22, roughness: 0.24, clearcoat: 0.9, clearcoatRoughness: 0.18 }),
      );
      artwork.add(core);

      const latitudeLines = new THREE.LineSegments(
        new THREE.WireframeGeometry(new THREE.SphereGeometry(1.235, 24, 16)),
        new THREE.LineBasicMaterial({ color: 0xc9d5ff, transparent: true, opacity: 0.25 }),
      );
      artwork.add(latitudeLines);

      const orbitColors = authPalette ? [0x94baff, 0x74dfd0, 0xc1a5ff]
        : reportPalette ? [0xc3a8ff, 0x8e7bff, 0x91cafa]
          : [0x9db5ff, 0x9c86ff, 0x75d9cc];
      const orbits: ThreeTypes.Group[] = [];
      orbitColors.forEach((color, index) => {
        const orbit = new THREE.Group();
        const ring = new THREE.Mesh(
          new THREE.TorusGeometry(1.72 + index * 0.2, 0.008, 8, 144),
          new THREE.MeshBasicMaterial({ color, transparent: true, opacity: 0.52 - index * 0.08 }),
        );
        ring.rotation.x = Math.PI / 2;
        orbit.rotation.set(0.46 + index * 0.58, 0.25 + index * 0.68, 0.34 + index * 0.4);
        orbit.add(ring);

        const node = new THREE.Mesh(
          new THREE.SphereGeometry(index === 1 ? 0.105 : 0.078, 24, 20),
          new THREE.MeshStandardMaterial({ color, emissive: color, emissiveIntensity: 0.45, roughness: 0.25 }),
        );
        node.position.set(1.72 + index * 0.2, 0, 0);
        orbit.add(node);
        artwork.add(orbit);
        orbits.push(orbit);
      });

      const starPositions = new Float32Array(270);
      for (let index = 0; index < starPositions.length; index += 3) {
        starPositions[index] = (Math.random() - 0.5) * 7.4;
        starPositions[index + 1] = (Math.random() - 0.5) * 6.2;
        starPositions[index + 2] = (Math.random() - 0.5) * 3 - 1.6;
      }
      const starGeometry = new THREE.BufferGeometry();
      starGeometry.setAttribute('position', new THREE.Float32BufferAttribute(starPositions, 3));
      const stars = new THREE.Points(starGeometry, new THREE.PointsMaterial({ color: 0x8298d7, size: 0.018, transparent: true, opacity: 0.62 }));
      scene.add(stars);

      this.scene = scene;
      this.camera = camera;
      this.artwork = artwork;
      this.renderer = renderer;
      this.resizeObserver = new ResizeObserver(() => this.resize());
      this.resizeObserver.observe(host);

      if (this.reducedMotion) {
        renderer.render(scene, camera);
      } else {
        renderer.setAnimationLoop((time) => {
          artwork.rotation.y = time * 0.00012;
          artwork.rotation.x = Math.sin(time * 0.00017) * 0.055;
          orbits.forEach((orbit, index) => {
            orbit.rotation.z += (index % 2 === 0 ? 1 : -1) * 0.00035;
          });
          renderer.render(scene, camera);
        });
      }
    } catch {
      // Keep the CSS glow as a graceful fallback when WebGL is unavailable.
    }
  }

  private resize(): void {
    const host = this.canvas.nativeElement.parentElement;
    if (!host || !this.renderer || !this.camera || !this.scene) return;
    const bounds = host.getBoundingClientRect();
    this.camera.aspect = Math.max(bounds.width, 1) / Math.max(bounds.height, 1);
    this.camera.updateProjectionMatrix();
    this.renderer.setSize(Math.max(bounds.width, 1), Math.max(bounds.height, 1), false);
    if (this.reducedMotion) this.renderer.render(this.scene, this.camera);
  }
}
