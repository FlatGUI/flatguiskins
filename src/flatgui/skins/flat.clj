; Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.

(ns ^{:doc    "Default 'flat' skin"
      :author "Denys Lebediev"}
flatgui.skins.flat
  ; TODO get rid of use
  (:use flatgui.awt
        flatgui.skins.skinbase
        flatgui.comlogic
        flatgui.paint)
  (:require [flatgui.awt :as awt]
            [flatgui.util.matrix :as m]))

(def round-rect-r (/ 1 16))

;;;
;;; Component
;;;

;(fgp/deflookfn component-look (:background :abs-position-matrix :clip-size)
;  (if (= :main (:id comp-property-map))
;    [(do
;       ;(if (= :main (:id comp-property-map)) (println " component-look for "
;       ;                                               (:id comp-property-map) " dirty-rects = " dirty-rects
;       ;                                               " abs pm = " abs-position-matrix
;       ;                                               " clip size = " clip-size))
;       (awt/setColor background))
;     ;(awt/fillRect 0 0 (m/x content-size) (m/y content-size))
;     (if (and dirty-rects abs-position-matrix)
;       ;Note: here it a single {:x .. :y .. :w .. :h ..} object, not a collection like in previous version. TODO rename parameter dirty-rects->dirty-rect
;       (let [ inter (flatgui.util.rectmath/rect&
;                      dirty-rects
;                      {:x (mx-x abs-position-matrix)
;                       :y (mx-y abs-position-matrix)
;                       :w (m/x clip-size)
;                       :h (m/y clip-size)})]
;         (if inter
;           (awt/fillRect
;             (- (:x inter) (mx-x abs-position-matrix))
;             (- (:y inter) (mx-y abs-position-matrix))
;             (:w inter)
;             (:h inter))))
;       ;(awt/fillRect 0 0 (m/x content-size) (m/y content-size))
;       []
;       )]
;    [(awt/setColor background)
;     (awt/fillRect 0 0 (m/x content-size) (m/y content-size))]
;    ))
(deflookfn component-look (:background :abs-position-matrix :clip-size)
           (awt/setColor background)
           (awt/fillRect 0 0 (m/x content-size) (m/y content-size)))


;;;
;;; Common label-look-impl for various components containing text
;;;

(defn get-label-text-x [interop w text h-alignment]
  (condp = h-alignment
    :left 0
    :right (- w (flatgui.awt/sw interop text))
    (/ (- w (flatgui.awt/sw interop text)) 2)))

(defn get-label-text-y
  ([interop h v-alignment font]
   (condp = v-alignment
     :top (flatgui.awt/sasc interop font)
     :bottom (- h (- (flatgui.awt/sh interop font) (flatgui.awt/sasc interop font)))
     (+ (- (/ h 2) (/ (flatgui.awt/sh interop font) 2)) (flatgui.awt/sasc interop font))))
  ([interop h v-alignment] (get-label-text-y interop h v-alignment nil)))

(defn label-look-impl [interop foreground text h-alignment v-alignment left top w h]
  [(flatgui.awt/setColor foreground)
   (let [dx (get-label-text-x interop w text h-alignment)
         dy (get-label-text-y interop h v-alignment)]
     (flatgui.awt/drawString text (+ left dx) (+ top dy)))
   ;(drawLine 0 (get-label-text-y interop h v-alignment) w (get-label-text-y interop h v-alignment))
   ])

(deflookfn label-look (:text :h-alignment :v-alignment :h-margin :v-margin)
  ;;TODO icon
  (label-look-impl interop foreground text h-alignment v-alignment h-margin v-margin (- w h-margin h-margin) (- h v-margin v-margin)))


;;;; TODO !!!! get-caret-x is duplicated: widget and skin. Find place for it single
(defn- get-caret-x [interop text caret-pos]
  (flatgui.awt/sw interop (if (< caret-pos (.length text)) (subs text 0 caret-pos) text)))

(defn- text-str-h [interop] (flatgui.awt/sh interop))

(defn- get-line-y [interop caret-line] (* caret-line (text-str-h interop)))

(deflookfn caret-look (:model :foreground :first-visible-symbol :v-margin :h-margin :multiline :v-alignment :h-alignment)
  (let [line (:caret-line model)
        trunk-text (subs (nth (:lines model) line) first-visible-symbol)
        trunk-caret-pos (- (:caret-line-pos model) first-visible-symbol)
        line-h (text-str-h interop)
        caret-y (if multiline
                  (+ (get-line-y interop line) v-margin)
                  (+ v-margin (- (get-label-text-y interop (- h (* v-margin 2)) v-alignment) (flatgui.awt/sasc interop))))
        xc (+ h-margin (get-label-text-x interop (- w (* h-margin 2)) (:text model) h-alignment) (get-caret-x interop trunk-text trunk-caret-pos))
        caret-y2 (flatgui.awt/-px (+ caret-y line-h) 2)]
    [(setColor foreground)
     (drawLine xc caret-y xc caret-y2)
     ;(drawLine 0 (get-label-text-y interop (- h (* v-margin 2)) v-alignment) w (get-label-text-y interop (- h (* v-margin 2)) v-alignment))
     ]))

(deflookfn textfield-look-impl (:foreground :text :h-alignment :v-alignment :caret-visible :theme :model :first-visible-symbol :multiline :h-margin :v-margin)
  (let [selection-start-in-line (if (> (:selection-mark model) (:caret-pos model)) (:caret-line-pos model) (:selection-mark-line-pos model))
        selection-end-in-line (if (> (:selection-mark model) (:caret-pos model)) (:selection-mark-line-pos model) (:caret-line-pos model))
        sstart-line (min (:selection-mark-line model) (:caret-line model))
        send-line (max (:selection-mark-line model) (:caret-line model))
        lines (:lines model)
        line-h (text-str-h interop)
        line-infos (map
                     (fn [i]
                       (let [line-text (nth lines i)]
                         {:line line-text
                          :y (* i line-h)
                          :line-sstart (if (= i sstart-line) selection-start-in-line 0)
                          :line-send (cond
                                       (and (>= i sstart-line) (< i send-line)) (.length line-text)
                                       (= i send-line) selection-end-in-line
                                       :else 0)}))
                     (range 0 (count lines)))]
    (conj
      (mapv
        (fn [line-info]
          (let [trunk-text (subs (:line line-info) first-visible-symbol)
                caret-pos (:caret-pos model)
                selection-mark (:selection-mark model)]
            [(if (not= caret-pos selection-mark)
               [(setColor (:prime-5 theme))
                (let [sstart (:line-sstart line-info)
                      send (:line-send line-info)]
                  (if (not= sstart send)
                    (let [x1 (get-caret-x interop trunk-text sstart)
                          x2 (get-caret-x interop trunk-text send)]
                      (fillRect (+ h-margin x1) (+ v-margin (:y line-info)) (- x2 x1) line-h))))])
             (if multiline
               (label-look-impl interop foreground trunk-text h-alignment v-alignment h-margin (+ (:y line-info) v-margin) (- w (* h-margin 2)) line-h)
               (label-look-impl interop foreground trunk-text h-alignment v-alignment h-margin v-margin (- w (* h-margin 2)) (- h (* v-margin 2))))]))
        line-infos)
      (if caret-visible (call-look caret-look)))))


;;;;

(defmacro has-focus [] `(= :has-focus (:mode ~'focus-state)))

(defn draw-component-rect
  ([x y w h panel-color component-color]
   [(setColor component-color)
    (drawRoundRect x y w h round-rect-r)])
  ([w h panel-color component-color]
   (draw-component-rect 0 0 w h panel-color component-color)))

(defn fill-component-rect
  ([x y w h panel-color component-color]
   [(setColor component-color)
    (fillRoundRect x y w h round-rect-r)])
  ([w h panel-color component-color]
    (fill-component-rect 0 0 w h panel-color component-color)))

(defn draw-leftsmoothbutton-component-rect [w h belongs-to-focused-editor theme background]
  (let [g (if belongs-to-focused-editor 2 1)]
    [(fill-component-rect w h nil (if belongs-to-focused-editor (:focused theme) (:prime-2 theme)))
     (fillRect 0 0 (* round-rect-r 2) h)
     (fill-component-rect (awt/+px 0 g) (awt/+px 0 g) (awt/-px w (* g 2)) (awt/-px h (* g 2)) nil background)
     (fillRect 0 (awt/+px 0 g) (* round-rect-r 2) (awt/-px h (* g 2)))]))

(defn draw-leftsmoothbutton-top-component-rect [w h belongs-to-focused-editor theme background]
  (let [g (if belongs-to-focused-editor 2 1)]
    [(fill-component-rect w h nil (if belongs-to-focused-editor (:focused theme) (:prime-2 theme)))
     (fillRect 0 0 (* round-rect-r 2) h)
     (fillRect (- w (* round-rect-r 2)) (/ h 2) (* round-rect-r 2) (/ h 2))
     (fill-component-rect (awt/+px 0 g) (awt/+px 0 g) (awt/-px w (* g 2)) (awt/-px h (* g 2)) nil background)
     (fillRect 0 (awt/+px 0 g) (* round-rect-r 2) (awt/-px h g))
     (fillRect 0 (/ h 2) (awt/-px w g) (/ h 2))]))

(defn draw-leftsmoothbutton-btm-component-rect [w h belongs-to-focused-editor theme background]
  (let [g (if belongs-to-focused-editor 2 1)]
    [(fill-component-rect w h nil (if belongs-to-focused-editor (:focused theme) (:prime-2 theme)))
     (fillRect 0 0 (* round-rect-r 2) h)
     (fillRect (- w (* round-rect-r 2)) 0 (* round-rect-r 2) (/ h 2))
     (fill-component-rect (awt/+px 0 g) (awt/+px 0 g) (awt/-px w (* g 2)) (awt/-px h (* g 2)) nil background)
     (fillRect 0 0 (* round-rect-r 2) (awt/-px h g))
     (fillRect 0 0 (awt/-px w g) (/ h 2))]))

;;;
;;; Panel
;;;

(deflookfn panel-look (:background)
           (flatgui.awt/setColor background)
           (fillRect 0 0 w h))


;;;
;;; Buttons
;;;

(deflookfn rollover-button-look (:pressed :theme :has-mouse)
           (let [bc (if pressed
                      (:prime-2 theme)
                      (:prime-1 theme))]
             ;; TODO there should be :background evolver fn instead of this
             [(if has-mouse (fill-component-rect w h (:prime-3 theme) bc))
              (call-look label-look)]))

(deflookfn regular-button-look (:theme :has-mouse :pressed)
           (let [bc (if pressed
                      (:prime-2 theme)
                      (:prime-1 theme))]
             [(fill-component-rect w h (:prime-3 theme) bc)
              (call-look label-look)]))


;;;
;;; Spinner
;;;

(defn arrow [x y w h theme up color]
  (let [ye (if up 0.5 0.375)
        ym (if up 0.375 0.5)
        wpx (* w (/ 1 (awt/px)))
        w (if (pos? (mod wpx 2)) w (awt/-px w))
        lx1 (+ x (awt/+px (* w 0.25)))
        ly1 (+ y (awt/+px (* h ye)))
        lx2 (+ x (awt/+px (* w 0.5)))
        ly2 (+ y (awt/+px (* h ym)))
        lx3 (+ x (awt/+px (* w 0.75)))
        ly3 (+ y (awt/+px (* h ye)))]
    [(drawLine lx1 ly1 lx2 ly2)
     (drawLine lx2 ly2 lx3 ly3)
     (setColor (mix-colors color (:engaged theme)))
     (drawLine lx1 (+px ly1) lx2 (+px ly2))
     (drawLine lx2 (+px ly2) lx3 (+px ly3))]))


(deflookfn spinner-up-look (:pressed :has-mouse :theme :belongs-to-focused-editor)
 (draw-leftsmoothbutton-top-component-rect (awt/-px w 1) h belongs-to-focused-editor theme (if pressed (:prime-2 theme) (:prime-1 theme)))
 (setColor (:prime-4 theme))
 (let [lx1 (* w 0.375)
       ly1 (- h (* w 0.0625))
       lx2 (* w 0.5)
       ly2 (- h (* w 0.25))
       lx3 (+px (* w 0.625))
       ly3 ly1
       dd (/ (- w h) 2)]
   ;(arrow-up lx1 ly1 lx2 ly2 lx3 ly3 theme (:prime-4 theme) (:prime-1 theme))
   (arrow dd 0 h h theme true (:prime-4 theme))
   ))

(deflookfn spinner-down-look (:pressed :has-mouse :theme :belongs-to-focused-editor)
 (draw-leftsmoothbutton-btm-component-rect (awt/-px w 1) h belongs-to-focused-editor theme (if pressed (:prime-2 theme) (:prime-1 theme)))
 (setColor (:prime-4 theme))
 (let [lx1 (* w 0.375)
       ly1 (* w 0.0625)
       lx2 (* w 0.5)
       ly2 (* w 0.25)
       lx3 (+px (* w 0.625))
       ly3 ly1
       dd (/ (- w h) 2)]
   ;(arrow-down lx1 ly1 lx2 ly2 lx3 ly3 theme (:prime-4 theme) (:prime-1 theme))
   (arrow dd 0 h h theme false (:prime-4 theme))
   ))

(deflookfn leftsmooth-editor-look (:has-mouse :theme :focus-state :background :editable)
  (let [g (if (has-focus) 2 1)]
    [(fill-component-rect (+ w round-rect-r) h nil (if (has-focus) (:focused theme) (:prime-2 theme)))
     (fill-component-rect (awt/+px 0 g) (awt/+px 0 g) (+ w round-rect-r) (awt/-px h (* 2 g)) nil background)
     (call-look textfield-look-impl)]))

;;;
;;; Combo Box
;;;

(deflookfn combobox-arrow-button-look (:has-mouse :pressed :theme :belongs-to-focused-editor)
           (draw-leftsmoothbutton-component-rect (awt/-px w 1) h belongs-to-focused-editor theme (if pressed (:prime-2 theme) (:prime-1 theme)))
           (setColor (:prime-4 theme))
           ;(let [lx1 (* w 0.375)
           ;      ly1 (* w 0.375)
           ;      lx2 (* w 0.5)
           ;      ly2 (* w 0.5)
           ;      lx3 (* w 0.625)
           ;      ly3 (* w 0.375)]
           ;  [(setColor (:prime-4 theme))
           ;   (drawLine lx1 (+px ly1) lx2 (+px ly2))
           ;   (drawLine lx2 (+px ly2) lx3 (+px ly3))
           ;   (setColor (mix-colors (:prime-4 theme) (:prime-1 theme)))
           ;   (drawLine lx1 ly1 lx2 ly2)
           ;   (drawLine lx2 ly2 lx3 ly3)
           ;   (drawLine lx1 (+px ly1 2) (-px lx2) (+px ly2 1))
           ;   (drawLine (+px lx2) (+px ly2 1) lx3 (+px ly3 2))])
           (arrow (/ h 4) 0 (/ h 2) h theme false (:prime-4 theme))
           )

(deflookfn dropdown-content-look (:theme)
               (call-look component-look)
               (awt/setColor (:prime-6 theme))
               (awt/drawRect 0 0 w- h-))

;;;
;;; Scroll Bar
;;;

(deflookfn scroller-look (:has-mouse :theme)
           (setColor (:extra-1 theme))
           (let [vertical (< w h)]
             (if vertical
               (let [i (* 0.5 w)
                     g (* 0.25 w)]
                 [(fillOval g g i i)
                  (fillRect g i i (-px (- h w)))
                  (fillOval g (- h w-) i i)])
               (let [i (* 0.5 h)
                     g (* 0.25 h)]
                 [(fillOval g g i i)
                  (fillRect i g (-px (- w h)) i)
                  (fillOval (- w h) g i i)]))))

(deflookfn scrollbar-look (:theme :orientation)
           (setColor (:extra-1 theme))
           (if (= :vertical orientation)
             (let [i (* 0.5 w)
                   g (* 0.25 w)]
               [(fillOval 0 0 w w)
                (fillRect 0 (/ w 2) w (- h w))
                (fillOval 0 (- h w) w w)
                (setColor (:extra-2 theme))
                (fillOval g g i i)
                (fillRect g i i (-px (- h w)))
                (fillOval g (- h w-) i i)])
             (let [i (* 0.5 h)
                   g (* 0.25 h)]
               [(fillOval 0 0 h h)
                (fillRect (/ h 2) 0 (- w h) h)
                (fillOval (- w h) 0 h h)
                (setColor (:extra-2 theme))
                (fillOval g g i i)
                (fillRect i g (-px (- w h)) i)
                (fillOval (- w h) g i i)
                ])))


;;;
;;; Text Field
;;;

(deflookfn textfield-look (:has-mouse :focus-state :theme :paint-border :background)
           (if paint-border
             (let [g (if (has-focus) 2 1)]
               [(fill-component-rect w h nil (if (has-focus) (:focused theme) (:prime-2 theme)))
                (fill-component-rect (awt/+px 0 g) (awt/+px 0 g) (awt/-px w (* 2 g)) (awt/-px h (* 2 g)) nil background)])
             (fill-component-rect w h nil background))
           (call-look textfield-look-impl))


;;;
;;; Rich text component
;;;

(deflookfn textrich-look-impl (:rendition :font :foreground :margin)
           (loop [r [(flatgui.awt/setColor foreground)]
                  y 0
                  row-index 0]
             (if (< row-index (count (:rendition rendition)))
               (let [row (nth (:rendition rendition) row-index)
                     row-h (:h row)]
                 (recur
                   (loop [rr r
                          x margin
                          e 0]
                     (if (< e (count (:primitives row)))
                       (let [p (nth (:primitives row) e)
                             p-font (if-let [sf (:font (:style p))] sf font)
                             cmd-&-w (condp = (:type p)

                                       :string [(flatgui.awt/drawString (:data p) x (+ y (get-label-text-y interop row-h :center p-font)))
                                                (flatgui.awt/sw interop (:data p) p-font)]

                                       :image [(flatgui.awt/drawImage (:data p) x y) (:w (:size (:style p)))]

                                       :video (let [w (:w (:size (:style p)))
                                                    h (:h (:size (:style p)))]
                                                [(flatgui.awt/fitVideo (:data p) x y h h) w])

                                       [(flatgui.awt/drawString "?" x (get-label-text-y interop row-h :center font))
                                        (flatgui.awt/sw interop "?" font)])]
                         (recur
                           (conj rr (first cmd-&-w))
                           (+ x (second cmd-&-w))
                           (inc e)))
                       rr))
                   (+ y row-h)
                   (inc row-index)))
               (conj
                 r
                 (let [cc (:caret-coords rendition)
                       lx (+ (nth cc 0) margin)]
                   (flatgui.awt/drawLine lx (nth cc 1) lx (+ (nth cc 1) (nth cc 2))))))))

(deflookfn textrich-look (:focus-state :theme :background :paint-border)
           (if paint-border
             (let [g (if (has-focus) 2 1)]
               [(fill-component-rect w h nil (if (has-focus) (:focused theme) (:prime-2 theme)))
                (fill-component-rect (awt/+px 0 g) (awt/+px 0 g) (awt/-px w (* 2 g)) (awt/-px h (* 2 g)) nil background)])
             (fill-component-rect w h nil background))
           (call-look textrich-look-impl))

;;;
;;; Rich text component
;;;

(defn- add-caret [rr caret-x y line-h]
  (if caret-x
    (conj rr (flatgui.awt/drawLine caret-x y caret-x (+ y line-h)))
    rr))

(defn- add-selection [rr s-start s-end y line-h theme]
  (if (and s-start s-end)
    (vec (concat [(setColor (:prime-5 theme)) (flatgui.awt/fillRect s-start y (- s-end s-start) line-h)] rr))
    rr))

(deflookfn textfield2-look-impl (:model :font :foreground :theme :margin)
  (let [;_ (println "----------------------------------- look --------------------------------------")
        lines (:lines model)
        line-count (count lines)]
    (loop [r [(flatgui.awt/setColor foreground)]
           y 0
           line-index 0]
      (if (< line-index line-count)
        (let [line (nth lines line-index)
              primitives (:primitives line)
              line-h (:h line)]
          (recur
            (loop [rr r
                   ;x margin
                   e 0
                   caret-x nil
                   s-start nil
                   s-end nil]
              (if (< e (count primitives))
                (let [p (nth primitives e)

                      p-font (if-let [sf (:font (:style p))] sf font)
                      ;cmd-&-w (condp = (:type p)
                      ;
                      ;          :string [(flatgui.awt/drawString (:data p) x (+ y (get-label-text-y interop line-h :center p-font)))
                      ;                   (flatgui.awt/sw interop (:data p) p-font)]
                      ;
                      ;          :image [(flatgui.awt/drawImage (:data p) x y) (:w (:size (:style p)))]
                      ;
                      ;          :video (let [w (:w (:size (:style p)))
                      ;                       h (:h (:size (:style p)))]
                      ;                   [(flatgui.awt/fitVideo (:data p) x y h h) w])
                      ;
                      ;          [(flatgui.awt/drawString "?" x (get-label-text-y interop line-h :center font))
                      ;           (flatgui.awt/sw interop "?" font)])]
                      x (:x p)
                      cmd (condp = (:type p)

                                :string (flatgui.awt/drawString (:data p) x (+ y (get-label-text-y interop line-h :center p-font)))

                                :image (flatgui.awt/drawImage (:data p) x y)

                                :video (let [w (:w (:size (:style p)))
                                             h (:h (:size (:style p)))]
                                         (flatgui.awt/fitVideo (:data p) x y w h))

                                (flatgui.awt/drawString "?" x (get-label-text-y interop line-h :center font)))]

                  (recur
                    (conj rr cmd);(conj rr (first cmd-&-w))
                    ;(+ x (:w p));(+ x (second cmd-&-w))
                    (inc e)
                    (if-let [cx (:caret-x p)] (+ x cx))
                    (if-let [ss (:s-start p)] (+ x ss))
                    (if-let [se (:s-end p)] (+ x se))
                    ))

                (->
                  (add-selection rr s-start s-end y line-h theme)
                  (add-caret caret-x y line-h))))

            (+ y line-h)
            (inc line-index)))

        ;(conj
        ;  r
        ;  (let [cc (:caret-coords rendition)
        ;        lx (+ (nth cc 0) margin)]
        ;    (flatgui.awt/drawLine lx (nth cc 1) lx (+ (nth cc 1) (nth cc 2)))))
        r

        ))))

(deflookfn textfield2-look (:focus-state :theme :background :paint-border)
  (if paint-border
    (let [g (if (has-focus) 2 1)]
      [(fill-component-rect w h nil (if (has-focus) (:focused theme) (:prime-2 theme)))
       (fill-component-rect (awt/+px 0 g) (awt/+px 0 g) (awt/-px w (* 2 g)) (awt/-px h (* 2 g)) nil background)])
    (fill-component-rect w h nil background))
  (call-look textfield2-look-impl))

;;;
;;; Check Box
;;;

(deflookfn checkbox-look (:theme :has-mouse :pressed :focus-state :foreground :v-alignment :h-alignment :text)
 [(if (has-focus)
    [(fill-component-rect h h (:prime-3 theme) (:focused theme))
     (fill-component-rect (awt/+px 0 2) (awt/+px 0 2) (awt/-px h 4) (awt/-px h 4) (:prime-3 theme) (:prime-1 theme))]
    (fill-component-rect h h (:prime-3 theme) (:prime-1 theme)))
  (if pressed
    (let [lx1 (awt/+px (* h 0.25))
          ly1 (awt/+px (* h 0.375))
          lx2 (awt/+px (* h 0.375))
          ly2 (awt/+px (* h 0.5))
          lx3 (awt/+px (* h 0.625))
          ly3 (awt/+px (* h 0.25))]
      [(setColor (:prime-4 theme))
       (drawLine lx1 ly1 lx2 ly2)
       (drawLine lx2 ly2 lx3 ly3)
       (setColor (mix-colors (:prime-4 theme) (:engaged theme)))
       (drawLine lx1 (+px ly1) lx2 (+px ly2))
       (drawLine lx2 (+px ly2) lx3 (+px ly3))]))]
 (label-look-impl interop foreground text h-alignment v-alignment h 0 w h))

;;;
;;; Slider
;;;

(deflookfn sliderhandlebase-look (:theme :side-gap :orientation :sliderhandle-position)
           (setColor (:prime-4 theme))
           (if (= :vertical orientation)
             (let [left (/ (- w side-gap) 2)
                   hy (flatgui.util.matrix/mx-y sliderhandle-position)]
               [(fillOval left side-gap side-gap side-gap)
                (fillRect left (+ (/ side-gap 2) side-gap) side-gap (- h (* 3 side-gap)))
                (setColor (:prime-2 theme))
                (fillOval left (- h side-gap side-gap) side-gap side-gap)
                (fillRect left (+ hy side-gap) side-gap (- h (* 2.25 side-gap) hy))])
             (let [top (/ (- h side-gap) 2)
                   hx (flatgui.util.matrix/mx-x sliderhandle-position)]
               [(fillRect (+ (/ side-gap 2) side-gap) top (- w (* 3 side-gap)) side-gap)
                (fillOval (- w side-gap side-gap) top side-gap side-gap)
                (setColor (:prime-2 theme))
                (fillOval side-gap top side-gap side-gap)
                (fillRect (+ (/ side-gap 2) side-gap) top hx side-gap)])))

(deflookfn sliderhandle-look (:has-mouse :theme :belongs-to-focused-editor :orientation)
           (if (= :vertical orientation)
             (let [left (/ (- w h) 2)
                   r h]
               [(if belongs-to-focused-editor
                  [(setColor (:focused theme))
                   (fillOval left 0 r r)
                   (setColor (:prime-1 theme))
                   (fillOval (awt/+px left 2) (awt/+px 0 2) (awt/-px r 4) (awt/-px r 4))]
                  [(setColor (:prime-1 theme))
                   (fillOval left 0 r r)])
                (setColor (:prime-4 theme))
                (let [d (* h 0.46875)]
                  (fillOval (+px (- (/ w 2) (/ d 2))) (+px (- (/ h 2) (/ d 2))) (-px d) (-px d)))])
             (let [top (/ (- h w) 2)
                   r w]
               [(if belongs-to-focused-editor
                  [(setColor (:focused theme))
                   (fillOval 0 top r r)
                   (setColor (:prime-1 theme))
                   (fillOval (awt/+px 0 2) (awt/+px top 2) (awt/-px r 4) (awt/-px r 4))]
                  [(setColor (:prime-1 theme))
                   (fillOval 0 top r r)])
                (setColor (:prime-4 theme))
                (let [d (* w 0.46875)]
                  (fillOval (+px (- (/ w 2) (/ d 2))) (+px (- (/ h 2) (/ d 2))) (-px d) (-px d)))])))

;;;
;;; Table
;;;

(deflookfn columnheader-look (:theme :has-mouse :mouse-down)
           (setColor (if mouse-down (:extra-1 theme) (:extra-2 theme)))
           (fillRect 0 (px) (-px w 1) (-px h 2))
           (call-look label-look))

(deflookfn tableheader-look (:theme :mouse-down)
  (awt/setColor (:prime-4 theme))
  (awt/fillRect 0 0 w h))

(deflookfn tablecell-look (:theme :anchor :text :h-alignment :v-alignment :foreground :screen-col)
               [(awt/setColor (:prime-4 theme))
                (awt/drawRect 0 0 w- h-)
                (awt/setColor background)
                (if (= screen-col 0) (awt/fillRect (awt/px) 0 (awt/-px w-) h-) (awt/fillRect 0 0 w- h-))
                (label-look-impl interop foreground text h-alignment v-alignment 0 0 w h)
                (if anchor (awt/setColor (:prime-2 theme)))
                (if anchor (awt/drawRect 0 0 (awt/-px w-) (awt/-px h-)))])

(deflookfn sorting-look (:theme :mode :degree)
               ;(flatgui.awt/setColor foreground)
               (let [text (if (> degree 0) (str degree))
                     tx (- w (awt/sw interop text))
                     hy (/ h 2)
                     ty (+ hy (awt/hsh interop))]
                 [ (cond
                     (= :asc mode)
                     (let [lx1 (* w 0.375)
                           ly1 (- (/ h 2) (* w 0.0625))
                           lx2 (* w 0.5)
                           ly2 (- (/ h 2) (* w 0.25))
                           lx3 (awt/+px (* w 0.625))
                           ly3 (- (/ h 2) (* w 0.0625))]
                       ;(flatgui.skins.flat/arrow-up lx1 ly1 lx2 ly2 lx3 ly3 theme (:prime-1 theme) (:extra-2 theme))
                       (setColor (:prime-1 theme))
                       (arrow (/ h 4) 0 (/ h 2) h theme true (:prime-1 theme))
                       )
                     (= :desc mode)
                     (let [lx1 (* w 0.375)
                           ly1 (+ (/ h 2) (* w 0.0625))
                           lx2 (* w 0.5)
                           ly2 (+ (/ h 2) (* w 0.25))
                           lx3 (awt/+px (* w 0.625))
                           ly3 (+ (/ h 2) (* w 0.0625))
                           ]
                       ;(flatgui.skins.flat/arrow-down lx1 ly1 lx2 ly2 lx3 ly3 theme (:prime-1 theme) (:extra-2 theme))
                       (setColor (:prime-1 theme))
                       (arrow (/ h 4) 0 (/ h 2) h theme false (:prime-1 theme))
                       ))
                  ;(if text (flatgui.awt/drawString text tx ty))
                  ]))

(defn- set-vfc-color [mode has-mouse theme]
  (awt/setColor (cond
                  (not= :none mode) (:prime-1 theme)
                  has-mouse (awt/mix-colors (:extra-1 theme) (:prime-1 theme))
                  :else (awt/mix-colors31 (:extra-1 theme) (:prime-1 theme)))))

(deflookfn filtering-look (:theme :mode :has-mouse)
               (let [fw (/ w 2)
                     btm (+ (/ h 2) (/ fw 2))
                     mid (- (/ h 2) (/ fw 4))
                     top (- (/ h 2) (/ fw 2))]
                 [(set-vfc-color mode has-mouse theme)
                  (awt/drawLine (* w 0.25) btm (* w 0.75) btm)
                  (awt/drawLine (* w 0.25) btm (* w 0.25) mid)
                  (awt/drawLine (* w 0.75) btm (* w 0.75) mid)
                  (awt/drawLine (* w 0.25) mid (* w 0.3125) top)
                  (awt/drawLine (* w 0.3125) top (* w 0.375) top)
                  (awt/drawLine (* w 0.375) top (* w 0.5) mid)
                  (awt/drawLine (* w 0.5) mid (* w 0.5625) top)
                  (awt/drawLine (* w 0.5625) top (* w 0.625) top)
                  (awt/drawLine (* w 0.625) top (* w 0.75) mid)
                  (awt/fillRect (* w 0.5625) (awt/+px top) (awt/+px (* w 0.125)) (- mid top))]))

(deflookfn grouping-look (:theme :mode :degree :has-mouse)
               (let [e (awt/+px 0 3) ; TODO (* 0.25 w)
                     t (/ e 2)
                     he (awt/+px 0 2) ; TODO (/ e 2)
                     ]
                 [(set-vfc-color mode has-mouse theme)
                  (awt/fillRect 0 t e e)
                  (awt/fillRect (+ e he) t e e)
                  (awt/fillRect (+ e he e he) t e e)]))


;;;
;;; Menu cell
;;;

(deflookfn menucell-look (:theme :anchor :id)
               [(awt/setColor background)
                ;; TODO 1 px is cut temporarily: until borders are introduced
                ;(flatgui.awt/fillRect 0 0 (m/x content-size) (m/y content-size))
                (awt/fillRect (awt/px) 0 (awt/-px (m/x content-size) 2) (awt/-px (m/y content-size)))
                (call-look label-look)])

;;;
;;; Toolbar
;;;

(deflookfn toolbar-look (:theme :background)
           [(setColor background)
            ;(fillRect (px) (px) w-2 h-2)
            (fillRect 0 0 w h)
            (setColor (:prime-2 theme))
            (drawLine (px) (* h 0.125) (px) (- h (* h 0.25)))
            ])

;;;
;;; Radio Button
;;;

(deflookfn radiobutton-look (:theme :pressed :focus-state :foreground :v-alignment :h-alignment :text)
           (let [cout (if (has-focus) (:focused theme) (:prime-6 theme))
                 cin (if pressed (:engaged theme) (:prime-6 theme))
                 r h]
             [(setColor cout)
              (fillOval 0 0 r r)
              (setColor (:prime-4 theme))
              (let [d (* r 0.75)]
                (fillOval (- (/ r 2) (/ d 2)) (- (/ r 2) (/ d 2)) d d))
              (if pressed
                [(setColor cin)
                 (let [d (* r 0.5)]
                   (fillOval (- (/ r 2) (/ d 2)) (- (/ r 2) (/ d 2)) d d))])
              (label-look-impl interop foreground text h-alignment v-alignment (if (= h-alignment :left) h 0) 0 w h)]))

;;;
;;; Window
;;;

(defn draw-focused? [focus-state]
  (#{:parent-of-focused :has-focus} (:mode focus-state)))

(deflookfn window-look (:theme :header-h :text :focus-state)
               [(call-look component-look)
                (if (draw-focused? focus-state)
                  [(awt/setColor (:prime-2 theme))
                   (awt/drawRect 0 0 w- h-)])
                (awt/setColor (if (draw-focused? focus-state) (:prime-4 theme) (:prime-2 theme)))
                (awt/fillRect 0 0 w header-h)
                (label-look-impl interop (if (draw-focused? focus-state) (:prime-1 theme) (:prime-4 theme)) text :left :center 0 0 w header-h)])


;;;
;;; skin-map
;;;

(def skin-map
  {:label label-look
   :spinner {:up spinner-up-look
             :down spinner-down-look
             :editor leftsmooth-editor-look}
   :button {:rollover rollover-button-look
            :regular regular-button-look}
   :combobox {:arrow-button combobox-arrow-button-look
              :editor leftsmooth-editor-look
              :dropdown {:content-pane dropdown-content-look}}
   :scrollbar {:scroller scroller-look
               :scrollbar scrollbar-look}
   :textfield textfield-look
   :textrich textrich-look
   :textfield2 textfield2-look
   :checkbox checkbox-look
   :radiobutton radiobutton-look
   :slider {:base sliderhandlebase-look
            :handle sliderhandle-look}
   :table {:tableheader tableheader-look
           :columnheader columnheader-look
           :sorting sorting-look
           :filtering filtering-look
           :grouping grouping-look
           :tablecell tablecell-look}
   :menu {:menucell menucell-look}
   :toolbar toolbar-look
   :window window-look
   :component component-look
   })