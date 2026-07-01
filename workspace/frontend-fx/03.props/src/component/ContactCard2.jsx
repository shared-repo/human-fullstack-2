
// function ContactCard2(props) {
//   return (
//     <div>
//       { props.children }
//     </div>
//   );
// }

function ContactCard2(props) {
    const { children } = props;
    return (
        <div>
        { children }
        </div>
    );
}

export default ContactCard2;