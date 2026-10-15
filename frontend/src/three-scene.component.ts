import { AfterViewInit, Component, ElementRef, Input, OnChanges, OnDestroy, SimpleChanges, ViewChild } from '@angular/core';
import * as THREE from 'three';

type SceneVariant = 'landing' | 'auth' | 'analyzer' | 'jobs' | 'applications' | 'history' | 'profile' | 'admin';

@Component({
  selector: 'app-three-scene',
  standalone: true,
  template: '<div #stage class="three-stage" aria-hidden="true"></div>',
  styles: [`
    :host { position: absolute; inset: 0; z-index: 0; display: block; overflow: hidden; pointer-events: none; }
    .three-stage { width: 100%; height: 100%; opacity: .95; }
    .three-stage canvas { width: 100%; height: 100%; display: block; }
    @media (prefers-reduced-motion: reduce) { .three-stage { opacity: .55; } }
  `],
})
export class ThreeSceneComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() theme: 'dark' | 'light' = 'dark';
  @Input() density = 190;
  @Input() variant: SceneVariant = 'landing';
  @ViewChild('stage', { static: true }) private stageRef!: ElementRef<HTMLDivElement>;

  private renderer?: THREE.WebGLRenderer;
  private scene?: THREE.Scene;
  private camera?: THREE.PerspectiveCamera;
  private world?: THREE.Group;
  private pageDecor?: THREE.Group;
  private frameId = 0;
  private resizeObserver?: ResizeObserver;
  private readonly pointer = new THREE.Vector2();
  private readonly clock = new THREE.Clock();
  private reducedMotion = false;

  ngAfterViewInit(): void {
    this.reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    this.createScene();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['variant'] && !changes['variant'].firstChange && this.world) this.replacePageDecor();
  }

  ngOnDestroy(): void {
    window.cancelAnimationFrame(this.frameId);
    window.removeEventListener('pointermove', this.onPointerMove);
    this.resizeObserver?.disconnect();
    this.scene?.traverse(object => {
      if (object instanceof THREE.Mesh || object instanceof THREE.Points || object instanceof THREE.Line) {
        object.geometry.dispose();
        const materials = Array.isArray(object.material) ? object.material : [object.material];
        materials.forEach(material => material.dispose());
      }
    });
    this.renderer?.dispose();
    this.renderer?.domElement.remove();
  }

  private createScene(): void {
    const host = this.stageRef.nativeElement;
    const width = Math.max(host.clientWidth, 1);
    const height = Math.max(host.clientHeight, 1);
    const dark = this.theme === 'dark';

    this.scene = new THREE.Scene();
    this.camera = new THREE.PerspectiveCamera(48, width / height, .1, 100);
    this.camera.position.set(0, 0, 8);
    this.renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true, powerPreference: 'high-performance' });
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.5));
    this.renderer.setSize(width, height, false);
    this.renderer.setClearColor(0x000000, 0);
    host.appendChild(this.renderer.domElement);

    this.world = new THREE.Group();
    this.scene.add(this.world);
    this.addParticleField(dark);
    this.addAiOrb(dark);
    this.addFloatingNodes(dark);
    this.replacePageDecor();

    this.resizeObserver = new ResizeObserver(() => this.resize());
    this.resizeObserver.observe(host);
    window.addEventListener('pointermove', this.onPointerMove, { passive: true });
    this.renderFrame();
  }

  private addParticleField(dark: boolean): void {
    if (!this.world) return;
    const positions = new Float32Array(this.density * 3);
    for (let index = 0; index < this.density; index += 1) {
      positions[index * 3] = (Math.random() - .5) * 15;
      positions[index * 3 + 1] = (Math.random() - .5) * 9;
      positions[index * 3 + 2] = (Math.random() - .5) * 6;
    }
    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    const material = new THREE.PointsMaterial({
      color: dark ? 0x7ea0ff : 0x315ef5,
      size: dark ? .035 : .025,
      transparent: true,
      opacity: dark ? .72 : .3,
      sizeAttenuation: true,
    });
    const particles = new THREE.Points(geometry, material);
    particles.name = 'particles';
    this.world.add(particles);
  }

  private addAiOrb(dark: boolean): void {
    if (!this.world) return;
    const orb = new THREE.Group();
    orb.name = 'orb';
    orb.position.set(3.25, .15, -.3);

    const core = new THREE.Mesh(
      new THREE.IcosahedronGeometry(1.05, 2),
      new THREE.MeshBasicMaterial({ color: dark ? 0x6c63ff : 0x496cf2, wireframe: true, transparent: true, opacity: dark ? .42 : .18 }),
    );
    orb.add(core);

    [1.48, 1.78, 2.08].forEach((radius, index) => {
      const ring = new THREE.Mesh(
        new THREE.TorusGeometry(radius, index === 0 ? .018 : .01, 8, 128),
        new THREE.MeshBasicMaterial({ color: index === 2 ? 0x22d3ee : 0x7893ff, transparent: true, opacity: dark ? .33 - index * .06 : .14 }),
      );
      ring.rotation.set(index * .65, index * .82, index * .35);
      orb.add(ring);
    });
    this.world.add(orb);
  }

  private addFloatingNodes(dark: boolean): void {
    if (!this.world) return;
    const colors = [0x315ef5, 0x7c4dff, 0x22d3ee, 0x16b981];
    const positions = [[-4.4, 2.1, 0], [-3.2, -2.4, -.5], [1.1, 2.7, -1], [4.6, -2.3, .2]];
    positions.forEach((position, index) => {
      const node = new THREE.Mesh(
        new THREE.OctahedronGeometry(index % 2 === 0 ? .12 : .09, 0),
        new THREE.MeshBasicMaterial({ color: colors[index], transparent: true, opacity: dark ? .9 : .5 }),
      );
      node.position.set(position[0], position[1], position[2]);
      node.name = `node-${index}`;
      this.world?.add(node);
    });
  }

  private replacePageDecor(): void {
    if (!this.world) return;
    if (this.pageDecor) {
      this.world.remove(this.pageDecor);
      this.disposeObject(this.pageDecor);
    }
    this.pageDecor = new THREE.Group();
    this.pageDecor.name = 'page-decor';
    this.pageDecor.position.z = -.6;
    this.world.add(this.pageDecor);

    if (this.variant === 'analyzer') this.addAnalyzerDecor(this.pageDecor);
    else if (this.variant === 'jobs') this.addJobsDecor(this.pageDecor);
    else if (this.variant === 'applications') this.addApplicationsDecor(this.pageDecor);
    else if (this.variant === 'history') this.addHistoryDecor(this.pageDecor);
    else if (this.variant === 'profile') this.addProfileDecor(this.pageDecor);
    else if (this.variant === 'admin') this.addAdminDecor(this.pageDecor);
    else if (this.variant === 'auth') this.addPortalDecor(this.pageDecor);
    else this.addConstellationDecor(this.pageDecor);
  }

  private addAnalyzerDecor(group: THREE.Group): void {
    const material = this.lineMaterial(0x6f8eff, .28);
    [-3.8, -2.55].forEach((x, index) => {
      const document = new THREE.Mesh(new THREE.BoxGeometry(.85, 1.1, .055), material.clone());
      document.position.set(x, index ? -1.8 : 1.65, 0);
      document.rotation.z = index ? .13 : -.12;
      group.add(document);
      for (let lineIndex = 0; lineIndex < 3; lineIndex += 1) {
        const line = new THREE.Mesh(new THREE.BoxGeometry(.5 - lineIndex * .06, .025, .075), this.lineMaterial(0x22d3ee, .38));
        line.position.set(x, document.position.y + .22 - lineIndex * .2, .06);
        line.rotation.z = document.rotation.z;
        group.add(line);
      }
    });
    group.add(this.connectionLine([[-3.4, 1.2, 0], [-2.9, .2, .2], [-2.55, -1.25, 0]], 0x7c4dff));
  }

  private addJobsDecor(group: THREE.Group): void {
    const positions = [[-4.2, 1.8, 0], [-3.1, .15, -.2], [-4, -1.7, .15]];
    positions.forEach((position, index) => {
      const card = new THREE.Mesh(new THREE.BoxGeometry(1.15, .72, .08), this.lineMaterial(index === 1 ? 0x7c4dff : 0x315ef5, .24));
      card.position.set(position[0], position[1], position[2]);
      card.rotation.set(.08 * index, -.1 * index, index % 2 ? -.08 : .08);
      group.add(card);
      const badge = new THREE.Mesh(new THREE.CircleGeometry(.1, 20), this.lineMaterial(index === 2 ? 0x16b981 : 0x22d3ee, .65));
      badge.position.set(position[0] - .38, position[1] + .13, position[2] + .06);
      group.add(badge);
    });
  }

  private addApplicationsDecor(group: THREE.Group): void {
    const points: Array<[number, number, number]> = [[-4.5, 1.8, 0], [-3.7, .7, .1], [-4.1, -.55, -.1], [-3.15, -1.65, .15]];
    group.add(this.connectionLine(points, 0x6f8eff));
    points.forEach((position, index) => {
      const node = new THREE.Mesh(new THREE.OctahedronGeometry(.15 + index * .018, 0), this.lineMaterial(index === 3 ? 0x16b981 : 0x7c4dff, .75));
      node.position.set(...position);
      group.add(node);
    });
  }

  private addHistoryDecor(group: THREE.Group): void {
    const heights = [.55, .9, .7, 1.3, 1.05, 1.65];
    heights.forEach((height, index) => {
      const bar = new THREE.Mesh(new THREE.BoxGeometry(.24, height, .24), this.lineMaterial(index > 3 ? 0x7c4dff : 0x315ef5, .32 + index * .045));
      bar.position.set(-4.5 + index * .42, -2.2 + height / 2, 0);
      group.add(bar);
    });
    group.add(this.connectionLine(heights.map((height, index) => [-4.5 + index * .42, -1.95 + height, .15] as [number, number, number]), 0x22d3ee));
  }

  private addProfileDecor(group: THREE.Group): void {
    const avatar = new THREE.Mesh(new THREE.SphereGeometry(.42, 24, 24), this.lineMaterial(0x6f8eff, .3));
    avatar.position.set(-3.8, -.1, 0);
    group.add(avatar);
    [0, 1].forEach(index => {
      const ring = new THREE.Mesh(new THREE.TorusGeometry(.75 + index * .3, .012, 8, 96), this.lineMaterial(index ? 0x22d3ee : 0x7c4dff, .36));
      ring.position.copy(avatar.position);
      ring.rotation.set(index ? 1.15 : .5, index ? .35 : .8, 0);
      group.add(ring);
    });
  }

  private addAdminDecor(group: THREE.Group): void {
    for (let row = 0; row < 3; row += 1) {
      for (let column = 0; column < 3; column += 1) {
        const cube = new THREE.Mesh(new THREE.BoxGeometry(.27, .27, .27), this.lineMaterial((row + column) % 2 ? 0x7c4dff : 0x315ef5, .24 + row * .08));
        cube.position.set(-4.4 + column * .52, 1.25 - row * .52, (row + column) * -.05);
        cube.rotation.set(.3, .4, .1);
        group.add(cube);
      }
    }
  }

  private addPortalDecor(group: THREE.Group): void {
    for (let index = 0; index < 4; index += 1) {
      const ring = new THREE.Mesh(new THREE.TorusGeometry(.55 + index * .24, .012, 8, 96), this.lineMaterial(index % 2 ? 0x7c4dff : 0x22d3ee, .3));
      ring.position.set(-3.7, -.5, -index * .12);
      ring.rotation.x = .35 + index * .12;
      group.add(ring);
    }
  }

  private addConstellationDecor(group: THREE.Group): void {
    const points: Array<[number, number, number]> = [[-4.6, 1.9, 0], [-3.8, .8, .2], [-4.35, -.25, 0], [-3.3, -1.35, .15]];
    group.add(this.connectionLine(points, 0x22d3ee));
    points.forEach(position => {
      const star = new THREE.Mesh(new THREE.TetrahedronGeometry(.12), this.lineMaterial(0x7c4dff, .7));
      star.position.set(...position);
      group.add(star);
    });
  }

  private connectionLine(points: Array<[number, number, number]>, color: number): THREE.Line {
    const geometry = new THREE.BufferGeometry().setFromPoints(points.map(point => new THREE.Vector3(...point)));
    return new THREE.Line(geometry, new THREE.LineBasicMaterial({ color, transparent: true, opacity: .42 }));
  }

  private lineMaterial(color: number, opacity: number): THREE.MeshBasicMaterial {
    return new THREE.MeshBasicMaterial({ color, transparent: true, opacity, wireframe: true });
  }

  private disposeObject(object: THREE.Object3D): void {
    object.traverse(child => {
      if (child instanceof THREE.Mesh || child instanceof THREE.Points || child instanceof THREE.Line) {
        child.geometry.dispose();
        const materials = Array.isArray(child.material) ? child.material : [child.material];
        materials.forEach(material => material.dispose());
      }
    });
  }

  private readonly onPointerMove = (event: PointerEvent): void => {
    this.pointer.x = (event.clientX / window.innerWidth) * 2 - 1;
    this.pointer.y = -(event.clientY / window.innerHeight) * 2 + 1;
  };

  private resize(): void {
    if (!this.renderer || !this.camera) return;
    const host = this.stageRef.nativeElement;
    const width = Math.max(host.clientWidth, 1);
    const height = Math.max(host.clientHeight, 1);
    this.camera.aspect = width / height;
    this.camera.updateProjectionMatrix();
    this.renderer.setSize(width, height, false);
  }

  private renderFrame = (): void => {
    if (!this.renderer || !this.scene || !this.camera || !this.world) return;
    const elapsed = this.clock.getElapsedTime();
    const orb = this.world.getObjectByName('orb');
    const particles = this.world.getObjectByName('particles');
    const pageDecor = this.world.getObjectByName('page-decor');

    if (!this.reducedMotion) {
      this.world.rotation.y += (this.pointer.x * .055 - this.world.rotation.y) * .025;
      this.world.rotation.x += (-this.pointer.y * .035 - this.world.rotation.x) * .025;
      if (orb) {
        orb.rotation.x = elapsed * .13;
        orb.rotation.y = elapsed * .19;
        orb.position.y = .15 + Math.sin(elapsed * .65) * .16;
      }
      if (particles) particles.rotation.y = elapsed * .012;
      if (pageDecor) {
        pageDecor.rotation.y = Math.sin(elapsed * .24) * .08;
        pageDecor.position.y = Math.sin(elapsed * .42) * .08;
        pageDecor.children.forEach((child, index) => {
          child.rotation.y += .0015 * ((index % 3) + 1);
        });
      }
      for (let index = 0; index < 4; index += 1) {
        const node = this.world.getObjectByName(`node-${index}`);
        if (node) {
          node.rotation.x = elapsed * (.3 + index * .07);
          node.rotation.y = elapsed * (.4 + index * .05);
          node.position.y += Math.sin(elapsed * .7 + index) * .0008;
        }
      }
    }

    this.renderer.render(this.scene, this.camera);
    if (!this.reducedMotion) this.frameId = window.requestAnimationFrame(this.renderFrame);
  };
}
