(ns re-val.subs
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [re-val.events :as h]
            [re-frame.core :as re-frame]))

(rf/reg-sub
 :form-valid?
 (fn [db [_ id]]
   (let [fields (get-in db [:data id :options :fields])
         data (get-in db [:data id :data])
         validation (get-in db [:data id :validation])]
     (h/valid-form? fields data validation db))))

(rf/reg-sub
 :get-form-data
 (fn [db [_ id]]
   (get-in db [:data id :data])))

(rf/reg-sub
 :get-form-field-data
 (fn [db [_ id k]]
   (get-in db [:data id :data k])))

(rf/reg-sub
 :get-form-field
 (fn [db [_ id k]]
   (get-in db [:data id :data k])))

(rf/reg-sub
 :get-validation-store
 (fn [db [_ id]]
   (get-in db [:data id :validation])))

(rf/reg-sub
 :the-db
 (fn [db _]
   db))

(rf/reg-sub
 :get-form-validation
 (fn [[_ form-id _] _]
   [(rf/subscribe [:get-form-data form-id])
    (rf/subscribe [:get-validation-store form-id])
    (rf/subscribe [:get-form-options form-id])
    (rf/subscribe [:the-db])])
 (fn [[data validation-store {:keys [fields] :as options} db] [_ form-id full?]]
   (if (= :full full?)
     ;;validate all fields
     (h/validate-all-fields (h/empty-validation-store fields) fields data db)
     ;;else, validate against existing validation store
     validation-store)))

(rf/reg-sub
 :get-form-invalid-fields
 (fn [[_ form-id full?] _]
   (rf/subscribe [:get-form-validation form-id (when full? :full)]))
 (fn [validation]
   (:invalid-fields validation)))

(rf/reg-sub
 :get-form-options
 (fn [db [_ id]]
   (get-in db [:data id :options])))

(rf/reg-sub
 :get-form-fields
 (fn [db [_ id]]
   (get-in db [:data id :options :fields])))
